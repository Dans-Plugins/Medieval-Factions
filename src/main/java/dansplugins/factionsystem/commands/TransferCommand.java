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
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.Objects;
import java.util.UUID;

/**
 * @author Callum Johnson
 */
public class TransferCommand extends SubCommand {

    public TransferCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
            "transfer", LOCALE_PREFIX + "CmdTransfer"
        }, true, true, false, true, ["mf.transfer"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
                "&c" + this.getText("UsageTransfer"),
                "UsageTransfer",
                false
            );
            return;
        }
        UUIDChecker uuidChecker = new UUIDChecker();
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("PlayerNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                true
            );
            return;
        }
        OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            target = Bukkit.getPlayer(args[0]);
            if (target == null) {
                this.playerService.sendMessage(
                    player, 
                    "&c" + this.getText("PlayerNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[0]),
                    true
                );
                return;
            }
        }
        if (!this.faction.isMember(targetUUID)) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("PlayerIsNotInYourFaction"),
                "PlayerIsNotInYourFaction",
                false
            );
            return;
        }
        if (targetUUID.equals(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotTransferToSelf"),
                "CannotTransferToSelf",
                false
            );
            return;
        }

        if (this.faction.isOfficer(targetUUID)) this.faction.removeOfficer(targetUUID); // Remove Officer (if there is one)

        // set owner
        this.faction.setOwner(targetUUID);
        this.playerService.sendMessage(
            player,
            "&b" + this.getText("OwnerShipTransferredTo", args[0]),
            Objects.requireNonNull(this.messageService.getLanguage().getString("OwnerShipTransferredTo")).replace("#name#", args[0]),
            true
        );
        if (target.isOnline() && target.getPlayer() != null) { // Message if we can :)
            this.playerService.sendMessage(
                target.getPlayer(),
                "&a" + this.getText("OwnershipTransferred", this.faction.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("'OwnershipTransferred")).replace("#name#", this.faction.getName()),
                true
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
        final List<String> membersInFaction = new ArrayList<>();
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            for (UUID uuid : playerFaction.getMemberList()) {
                Player member = Bukkit.getPlayer(uuid);
                if (member != null) {
                    membersInFaction.add(member.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], membersInFaction);
        }
    }
}