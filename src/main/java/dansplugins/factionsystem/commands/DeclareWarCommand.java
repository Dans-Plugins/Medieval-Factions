/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.factories.WarFactory;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.misc.ArgumentParser;

import java.util.List;
import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class DeclareWarCommand extends SubCommand {
    private final WarFactory warFactory;

    public DeclareWarCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, WarFactory warFactory) {
        super(new String[]{
                "declarewar", "dw", LOCALE_PREFIX + "CmdDeclareWar"
        }, true, true, true, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
        this.warFactory = warFactory;
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
        final String permission = "mf.declarewar";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + "Usage: /mf declarewar \"faction\""));
            } else {
                PlayerService.sendPlayerMessage(player, "UsageDeclareWar", true);
            }
            return;
        }

        ArgumentParser argumentParser = new ArgumentParser();
        List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + "Usage: /mf declarewar \"faction\" (quotation marks are required)"));
            } else {
                PlayerService.sendPlayerMessage(player, "UsageDeclareWar", true);
            }
            return;
        }

        String factionName = doubleQuoteArgs.get(0);

        final Faction opponent = getFaction(factionName);
        if (opponent == null) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("FactionNotFound")));
            } else {
                PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("FactionNotFound"))
                        .replaceAll("#faction#", String.join(" ", args)), false);
            }
            return;
        }

        if (opponent == faction) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarOnYourself")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDeclareWarOnYourself", true);
            }
            return;
        }

        if (faction.isEnemy(opponent.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("AlertAlreadyAtWarWith", opponent.getName())));
            } else {
                PlayerService.sendPlayerMessage(player, Objects.requireNonNull(MessageService.getLanguage().getString("AlertAlreadyAtWarWith")).replaceAll("#faction#", opponent.getName()), false);
            }
            return;
        }

        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getName())) {
                if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                    player.sendMessage(translate("&c" + getText("CannotDeclareWarOnVassal")));
                } else {
                    PlayerService.sendPlayerMessage(player, "CannotDeclareWarOnVassal", true);
                }
                return;
            }

            if (!faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = getFaction(opponent.getLiege());
                if (enemyLiege.calculateCumulativePowerLevelWithoutVassalContribution() <
                        enemyLiege.getMaximumCumulativePowerLevel() / 2) {
                    if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                        player.sendMessage(translate("&c" + getText("CannotDeclareWarIfLiegeNotWeakened")));
                    } else {
                        PlayerService.sendPlayerMessage(player, "CannotDeclareWarIfLiegeNotWeakened", true);
                    }
                }
            }
        }

        if (faction.isLiege(opponent.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarOnLiege")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDeclareWarOnLiege", true);
            }
            return;
        }

        if (faction.isAlly(opponent.getName())) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarOnAlly")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDeclareWarOnAlly", true);
            }
            return;
        }

        if (configService.getBoolean("allowNeutrality") && ((boolean) opponent.getFlags().getFlag("neutral"))) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarOnNeutralFaction")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDeclareWarOnNeutralFaction", true);
            }
            return;
        }

        if (configService.getBoolean("allowNeutrality") && ((boolean) faction.getFlags().getFlag("neutral"))) {
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&c" + getText("CannotDeclareWarIfNeutralFaction")));
            } else {
                PlayerService.sendPlayerMessage(player, "CannotDeclareWarIfNeutralFaction", true);
            }
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            warFactory.createWar(faction, opponent);
            if (!MedievalFactions.USE_NEW_LANGUAGE_FILE) {
                messageServer(translate("&c" + getText("HasDeclaredWarAgainst", faction.getName(), opponent.getName())));
            } else {
                sendMessageServer(Objects.requireNonNull(MessageService.getLanguage().getString("HasDeclaredWarAgainst"))
                        .replaceAll("#f_a#", faction.getName())
                        .replaceAll("#f_b#", opponent.getName()));
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