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
import dansplugins.factionsystem.utils.TabCompleteTools;
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
        }, true, true, true, false, ["mf.declarewar"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + "Usage: /mf declarewar \"faction\"",
                "UsageDeclareWar",
                false
            );
            return;
        }

        ArgumentParser argumentParser = new ArgumentParser();
        List<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            this.playerService.sendMessage(
                player,
                "&c" + "Usage: /mf declarewar \"faction\" (quotation marks are required)",
                "UsageDeclareWar",
                false
            );
            return;
        }

        String factionName = doubleQuoteArgs.get(0);

        final Faction opponent = this.getFaction(factionName);
        if (opponent == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }

        if (opponent == this.faction) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("CannotDeclareWarOnYourself"),
                "CannotDeclareWarOnYourself",
                false
            );
            return;
        }

        if (this.faction.isEnemy(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("AlertAlreadyAtWarWith"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAlreadyAtWarWith")).replace("#faction#", opponent.getName()),
                true
            );
            return;
        }

        if (this.faction.hasLiege() && opponent.hasLiege()) {
            if (this.faction.isVassal(opponent.getName())) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.getText("CannotDeclareWarOnVassal"),
                    "CannotDeclareWarOnVassal",
                    false
                );
                return;
            }

            if (!this.faction.getLiege().equalsIgnoreCase(opponent.getLiege())) {
                final Faction enemyLiege = getFaction(opponent.getLiege());
                if (enemyLiege.calculateCumulativePowerLevelWithoutVassalContribution() <
                        enemyLiege.getMaximumCumulativePowerLevel() / 2) {
                    this.playerService.sendMessage(
                        player,
                        "&c" + this.getText("CannotDeclareWarIfLiegeNotWeakened"),
                        "CannotDeclareWarIfLiegeNotWeakened",
                        false
                    );
                }
            }
        }

        if (this.faction.isLiege(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotDeclareWarOnLiege"),
                "CannotDeclareWarOnLiege",
                false
            );
            return;
        }

        if (this.faction.isAlly(opponent.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotDeclareWarOnAlly"),
                "CannotDeclareWarOnAlly",
                false
            );
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && ((boolean) opponent.getFlags().getFlag("neutral"))) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotDeclareWarOnNeutralFaction"),
                "CannotDeclareWarOnNeutralFaction",
                false
            );
            return;
        }

        if (this.configService.getBoolean("allowNeutrality") && ((boolean) faction.getFlags().getFlag("neutral"))) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotDeclareWarIfNeutralFaction"),
                "CannotDeclareWarIfNeutralFaction",
                false
            );
            return;
        }

        FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, opponent, player);
        Bukkit.getPluginManager().callEvent(warStartEvent);
        if (!warStartEvent.isCancelled()) {
            // Make enemies.
            this.faction.addEnemy(opponent.getName());
            opponent.addEnemy(faction.getName());
            warFactory.createWar(this.faction, opponent);
            this.messageServer(
                "&c" + this.getText("HasDeclaredWarAgainst", this.faction.getName(), opponent.getName()), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredWarAgainst"))
                    .replace("#f_a#", this.faction.getName())
                    .replace("#f_b#", opponent.getName())
            );

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
            final List<String> factionsAllowedtoWar = new ArrayList<>();
            Faction playerFaction = this.persistentData.getPlayersFaction(player.getUniqueId());
            ArrayList<String> playerEnemies = playerFaction.getEnemyFactions();
            ArrayList<String> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                // If the faction is not an ally and they are not already enemied to them
                if(!playerAllies.contains(faction.getName()) && !playerEnemies.contains(faction.getName()) && !faction.getName().equalsIgnoreCase(playerFaction.getName())) {
                    factionsAllowedtoWar.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWithAddQuotes(args[0], factionsAllowedtoWar);
        }
        return null;
    }
}