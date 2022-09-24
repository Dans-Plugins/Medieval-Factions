/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class VassalizeCommand extends SubCommand {
    private final Logger logger;

    public VassalizeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "Vassalize", LOCALE_PREFIX + "CmdVassalize"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.logger = logger;
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
        final String permission = "mf.vassalize";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessage(player, "&c" + getText("UsageVassalize")
                    , "UsageVassalize", false);
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            playerService.sendMessage(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true);
            return;
        }
        // make sure player isn't trying to vassalize their own faction
        if (faction.getName().equalsIgnoreCase(target.getName())) {
            playerService.sendMessage(player, "&c" + getText("CannotVassalizeSelf")
                    , "CannotVassalizeSelf", false);
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getName().equalsIgnoreCase(faction.getLiege())) {
            playerService.sendMessage(player, "&c" + getText("CannotVassalizeLiege")
                    , "CannotVassalizeLiege", false);
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            playerService.sendMessage(player, "&c" + getText("CannotVassalizeVassal")
                    , "CannotVassalizeVassal", false);
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = willVassalizationResultInLoop(faction, target);
        if (loopCheck == 1 || loopCheck == 2) {
            logger.debug("Vassalization was cancelled due to potential loop");
            return;
        }
        // add faction to attemptedVassalizations
        faction.addAttemptedVassalization(target.getName());

        // inform all players in that faction that they are trying to be vassalized
        messageFaction(target, translate("&a" +
                        getText("AlertAttemptedVassalization", faction.getName(), faction.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AlertAttemptedVassalization"))
                        .replace("#name#", faction.getName()));

        // inform all players in players faction that a vassalization offer was sent
        messageFaction(faction, translate("&a" + getText("AlertFactionAttemptedToVassalize", target.getName()))
                , Objects.requireNonNull(messageService.getLanguage().getString("AlertFactionAttemptedToVassalize"))
                        .replace("#name#", target.getName()));
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

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            String liegeName = current.getLiege();
            if (liegeName.equalsIgnoreCase("none")) return 0; // no loop will be formed
            if (liegeName.equalsIgnoreCase(potentialVassal.getName())) return 1; // loop will be formed
            current = persistentData.getFaction(liegeName);
            steps++;
        }
        return 2; // We don't know :/
    }
}