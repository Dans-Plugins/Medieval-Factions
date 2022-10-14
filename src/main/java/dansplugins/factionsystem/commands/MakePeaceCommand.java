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
import dansplugins.factionsystem.utils.TabCompleteTools;
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
        }, true, true, true, false, ["mf.makepeace"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageMakePeace"),
                "UsageMakePeace",
                false
            );
            return;
        }
        final Faction target = this.getFaction(String.join(" ", args));
        if (target == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        if (target == this.faction) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotMakePeaceWithSelf"),
                "CannotMakePeaceWithSelf",
                false
            );
            return;
        }
        if (this.faction.isTruceRequested(target.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("AlertAlreadyRequestedPeace"),
                "AlertAlreadyRequestedPeace",
                false
            );
            return;
        }
        if (!this.faction.isEnemy(target.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("FactionNotEnemy"),
                "FactionNotEnemy",
                false
            );
            return;
        }
        this.faction.requestTruce(target.getName());
        this.playerService.sendMessage(
            player,
            "&a" + this.getText("AttemptedPeace", target.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AttemptedPeace")).replace("#name#", target.getName()),
            true
        );
        this.messageFaction(
            target,
            this.translate("&a" + this.getText("HasAttemptedToMakePeaceWith", this.faction.getName(), target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasAttemptedToMakePeaceWith"))
                .replace("#f1#", this.faction.getName())
                .replace("#f2#", target.getName())
        );
        if (this.faction.isTruceRequested(target.getName()) && target.isTruceRequested(this.faction.getName())) {
            FactionWarEndEvent warEndEvent = new FactionWarEndEvent(this.faction, target);
            Bukkit.getPluginManager().callEvent(warEndEvent);
            if (!warEndEvent.isCancelled()) {
                // remove requests in case war breaks out again, and they need to make peace again
                this.faction.removeRequestedTruce(target.getName());
                target.removeRequestedTruce(this.faction.getName());

                // make peace between factions
                this.faction.removeEnemy(target.getName());
                target.removeEnemy(this.faction.getName());

                // TODO: set active flag in war to false

                // Notify
                this.messageServer(
                    "&a" + this.getText("AlertNowAtPeaceWith", this.faction.getName(), target.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAtPeaceWith"))
                        .replace("#p1#", this.faction.getName())
                        .replace("#p2#", target.getName())
                );
            }
        }

        // if faction was a liege, then make peace with all of their vassals as well
        if (target.isLiege()) {
            for (String vassalName : target.getVassals()) {
                this.faction.removeEnemy(vassalName);

                Faction vassal = this.getFaction(vassalName);
                vassal.removeEnemy(this.faction.getName());
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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            ArrayList<String> factionEnemies = playerFaction.getEnemyFactions();
            return TabCompleteTools.filterStartingWith(args[0], factionEnemies);
        }
    }
}