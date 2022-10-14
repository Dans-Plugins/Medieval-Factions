/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class DemoteCommand extends SubCommand {

    public DemoteCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "demote", LOCALE_PREFIX + "CmdDemote"
        }, true, true, false, true, ["mf.demote"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
                "&c" + this.getText("UsageDemote"),
                "UsageDemote", 
                false
            );
            return;
        }

        OfflinePlayer playerToBeDemoted = null;
        for (UUID uuid : this.faction.getMemberList()) {
            final OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(uuid);
            if (offlinePlayer.getName() == null) continue;
            if (offlinePlayer.getName().equalsIgnoreCase(args[0])) playerToBeDemoted = offlinePlayer;
        }

        if (playerToBeDemoted == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("PlayerByNameNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerByNameNotFound")).replace("#name#", args[0]), 
                true
            );
            return;
        }

        if (playerToBeDemoted.getUniqueId() == player.getUniqueId()) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("CannotDemoteSelf"),
                "CannotDemoteSelf", 
                false
            );
            return;
        }

        if (!this.faction.isOfficer(playerToBeDemoted.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("PlayerIsNotOfficerOfFaction"),
                "PlayerIsNotOfficerOfFaction", 
                false
            );
            return;
        }

        this.faction.removeOfficer(playerToBeDemoted.getUniqueId());

        if (playerToBeDemoted.isOnline()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("AlertDemotion"),
                "AlertDemotion",
                false
            );
        }
        this.playerService.sendMessage(
            player,
            "&c" + this.getText("PlayerDemoted"),
            Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerDemoted")).replace("#name#", playerToBeDemoted.getName()), 
            true
        );
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
        final List<String> officersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            for (UUID uuid : playerFaction.getOfficerList()) {
                Player officer = Bukkit.getPlayer(uuid);
                if (officer != null) {
                    officersInFaction.add(officer.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], officersInFaction);
        }
    }
}