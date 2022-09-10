/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarEndEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class MakePeaceCommand extends SubCommand {

    public MakePeaceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "makepeace", "mp", LOCALE_PREFIX + "CmdMakePeace"
        }, true, true, true, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.makepeace";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player,
                    "&c" + getText("UsageMakePeace")
                    , "UsageMakePeace", false);
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"),
                    Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                            .replace("#faction#", String.join(" ", args)), true);
            return;
        }
        if (target == faction) {
            playerService.sendMessageType(player, "&c" + getText("CannotMakePeaceWithSelf")
                    , "CannotMakePeaceWithSelf", false);
            return;
        }
        if (faction.isTruceRequested(target.getName())) {
            playerService.sendMessageType(player, "&c" + getText("AlertAlreadyRequestedPeace")
                    , "AlertAlreadyRequestedPeace", false);
            return;
        }
        if (!faction.isEnemy(target.getName())) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotEnemy")
                    , "FactionNotEnemy", false);
            return;
        }
        faction.requestTruce(target.getName());
        playerService.sendMessageType(player, "&a" + getText("AttemptedPeace", target.getName())
                , Objects.requireNonNull(messageService.getLanguage().getString("AttemptedPeace"))
                        .replace("#name#", target.getName()),
                true);
        messageFaction(target,
                translate("&a" + getText("HasAttemptedToMakePeaceWith", faction.getName(), target.getName())),
                Objects.requireNonNull(messageService.getLanguage().getString("HasAttemptedToMakePeaceWith"))
                        .replace("#f1#", faction.getName())
                        .replace("#f2#", target.getName()));
        if (faction.isTruceRequested(target.getName()) && target.isTruceRequested(faction.getName())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(this.faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (!warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again, and they need to make peace again
                faction.removeRequestedTruce(target.getName());
                target.removeRequestedTruce(faction.getName());

                // make peace between factions
                faction.removeEnemy(target.getName());
                target.removeEnemy(faction.getName());

                // TODO: set active flag in war to false

                // Notify
                messageServer("&a" + getText("AlertNowAtPeaceWith", faction.getName(), target.getName()),
                        Objects.requireNonNull(messageService.getLanguage().getString("AlertNowAtPeaceWith"))
                                .replace("#p1#", faction.getName())
                                .replace("#p2#", target.getName()));
            }
        }

        // if faction was a liege, then make peace with all of their vassals as well
        if (target.isLiege()) {
            for (String vassalName : target.getVassals()) {
                faction.removeEnemy(vassalName);

                Faction vassal = getFaction(vassalName);
                vassal.removeEnemy(faction.getName());
            }
        }
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }
}