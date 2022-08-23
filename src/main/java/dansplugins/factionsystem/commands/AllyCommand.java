/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class AllyCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AllyCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "ally", LOCALE_PREFIX + "CmdAlly"
        }, true, true, true, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
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
        final String permission = "mf.ally";

        if (!checkPermissions(player, permission)) {
            return;
        }

        if (args.length == 0) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("UsageAlly")));
            } else {
                PlayerService.sendPlayerMessage(player, "UsageAlly", true);
            }
            return;
        }

        // retrieve the Faction from the given arguments
        final Faction otherFaction = getFaction(String.join(" ", args));

        // the faction needs to exist to ally
        if (otherFaction == null) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("FactionNotFound")));
            } else {
                PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("FactionNotFound"))
                        .replaceAll("#faction#", String.join(" ", args)), false);
            }
            return;
        }

        // the faction can't be itself
        if (otherFaction == faction) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotAllyWithSelf")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotAllyWithSelf", true);
            }
            return;
        }

        // no need to allow them to ally if they're already allies
        if (faction.isAlly(otherFaction.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("FactionAlreadyAlly")));
            } else {
                PlayerService.sendPlayerMessage(player, "FactionAlreadyAlly", true);
            }
            return;
        }

        if (faction.isEnemy(otherFaction.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(ChatColor.RED + "That faction is currently at war with your faction.");
            } else {
                PlayerService.sendPlayerMessage(player, "FactionIsEnemy", true);
            }
            return;
        }

        if (faction.isRequestedAlly(otherFaction.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("AlertAlreadyRequestedAlliance")));
            } else {
                PlayerService.sendPlayerMessage(player, "AlertAlreadyRequestedAlliance", true);
            }
            return;
        }

        // send the request
        faction.requestAlly(otherFaction.getName());
        if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {

            messageFaction(
                    faction,
                    translate("&a" + getText("AlertAttemptedAlliance", faction.getName(), otherFaction.getName()))
            );

            messageFaction(
                    otherFaction,
                    translate("&a" + getText("AlertAttemptedAlliance", faction.getName(), otherFaction.getName()))
            );
        } else {
            sendMessageFaction(faction, Objects.requireNonNull(MessageService.getLanguage().getString("AlertAttemptedAlliance"))
                    .replaceAll("#faction_a#", faction.getName())
                    .replaceAll("#faction_b#", otherFaction.getName()));
            sendMessageFaction(otherFaction, Objects.requireNonNull(MessageService.getLanguage().getString("AlertAttemptedAlliance"))
                    .replaceAll("#faction_a#", faction.getName())
                    .replaceAll("#faction_b#", otherFaction.getName()));
        }
        // check if both factions are have requested an alliance
        if (faction.isRequestedAlly(otherFaction.getName()) && otherFaction.isRequestedAlly(faction.getName())) {
            // ally them
            faction.addAlly(otherFaction.getName());
            otherFaction.addAlly(faction.getName());
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                // message player's faction
                messageFaction(faction, translate("&a" + getText("AlertNowAlliedWith", otherFaction.getName())));

                // message target faction
                messageFaction(otherFaction, translate("&a" + getText("AlertNowAlliedWith", faction.getName())));
            } else {
                sendMessageFaction(faction, Objects.requireNonNull(MessageService.getLanguage().getString("AlertNowAlliedWith")).replaceAll("#faction#", otherFaction.getName()));
                sendMessageFaction(otherFaction, Objects.requireNonNull(MessageService.getLanguage().getString("AlertNowAlliedWith")).replaceAll("#faction#", faction.getName()));
            }

            // remove alliance requests
            faction.removeAllianceRequest(otherFaction.getName());
            otherFaction.removeAllianceRequest(faction.getName());
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