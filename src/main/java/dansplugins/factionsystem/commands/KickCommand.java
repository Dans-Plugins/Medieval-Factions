/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
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
public class KickCommand extends SubCommand {
    private final Logger logger;

    public KickCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "kick", LOCALE_PREFIX + "CmdKick"
        }, true, true, true, false, ["mf.kick"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (args.length == 0) {
            this.playerService.sendMessage(
                player, 
                "&c" + this.getText("UsageKick"),
                "UsageKick", 
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
        if (target.getUniqueId().equals(player.getUniqueId())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotKickSelf"),
                "CannotKickSelf",
                false
            );
            return;
        }
        if (this.faction.isOwner(targetUUID)) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotKickOwner"),
                "CannotKickOwner",
                false
            );
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(this.faction, target, player);
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            this.logger.debug("Kick event was cancelled.");
            return;
        }
        if (this.faction.isOfficer(targetUUID)) {
            this.faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        this.ephemeralData.getPlayersInFactionChat().remove(targetUUID);
        this.faction.removeMember(targetUUID);
        this.messageFaction(
            this.faction,
            "&c" + this.getText("HasBeenKickedFrom", target.getName(), this.faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasBeenKickedFrom"))
                    .replace("#name#", args[0])
                    .replace("#faction#", this.faction.getName())
        );
        if (target.isOnline() && target.getPlayer() != null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("AlertKicked", player.getName()),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertKicked")).replace("#name#", player.getName()),
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