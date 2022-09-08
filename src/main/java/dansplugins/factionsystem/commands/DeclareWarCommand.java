/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

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

    public DeclareWarCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, WarFactory warFactory, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "declarewar", "dw", LOCALE_PREFIX + "CmdDeclareWar"
        }, true, true, true, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
            playerService.sendMessageType(player, "&c" + "Usage: /mf declarewar \"faction\"",
                    "UsageDeclareWar", false);
            return;
        }

        ArgumentParser argumentParser = new ArgumentParser();
        List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            playerService.sendMessageType(player, "&c" + "Usage: /mf declarewar \"faction\" (quotation marks are required)",
                    "UsageDeclareWar", false);
            return;
        }

        String factionName = doubleQuoteArgs.get(0);

        final Faction opponent = getFaction(factionName);
        if (opponent == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true);
            return;
        }

        if (opponent == faction) {
            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnYourself")
                    , "CannotDeclareWarOnYourself", false);
            return;
        }

        if (faction.isEnemy(opponent.getName())) {
            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnYourself")
                    , Objects.requireNonNull(messageService.getLanguage().getString("AlertAlreadyAtWarWith")).replace("#faction#", opponent.getName()), true);

            return;
        }

        if (faction.hasLiege() && opponent.hasLiege()) {
            if (faction.isVassal(opponent.getName())) {
                playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnVassal")
                        , "CannotDeclareWarOnVassal", false);
                return;
            }

            if (!faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = getFaction(opponent.getLiege());
                if (enemyLiege.calculateCumulativePowerLevelWithoutVassalContribution() <
                        enemyLiege.getMaximumCumulativePowerLevel() / 2) {
                    playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarIfLiegeNotWeakened")
                            , "CannotDeclareWarIfLiegeNotWeakened", false);
                }
            }
        }

        if (faction.isLiege(opponent.getName())) {
            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnLiege")
                    , "CannotDeclareWarOnLiege", false);
            return;
        }

        if (faction.isAlly(opponent.getName())) {
            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnAlly")
                    , "CannotDeclareWarOnAlly", false);
            return;
        }

        if (configService.getBoolean("allowNeutrality") && ((boolean) opponent.getFlags().getFlag("neutral"))) {
            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarOnNeutralFaction")
                    , "CannotDeclareWarOnNeutralFaction", false);
            return;
        }

        if (configService.getBoolean("allowNeutrality") && ((boolean) faction.getFlags().getFlag("neutral"))) {

            playerService.sendMessageType(player, "&c" + getText("CannotDeclareWarIfNeutralFaction")
                    , "CannotDeclareWarIfNeutralFaction", false);
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            warFactory.createWar(faction, opponent);
            messageServer("&c" + getText("HasDeclaredWarAgainst", faction.getName(), opponent.getName()), Objects.requireNonNull(messageService.getLanguage().getString("HasDeclaredWarAgainst"))
                    .replace("#f_a#", faction.getName())
                    .replace("#f_b#", opponent.getName()));

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