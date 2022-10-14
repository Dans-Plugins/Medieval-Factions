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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class UnclaimallCommand extends SubCommand {

    public UnclaimallCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "unclaimall", "ua", LOCALE_PREFIX + "CmdUnclaimall"
        }, false, [], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final Faction faction;
        if (args.length == 0) {
            // Self
            if (!(sender instanceof Player)) {
                this.playerService.sendMessage(
                    sender, 
                    this.getText("OnlyPlayersCanUseCommand"),
                    "OnlyPlayersCanUseCommand", 
                    false
                );
                return;
            }
            if (!(this.checkPermissions(sender, "mf.unclaimall"))) return;
            faction = this.getPlayerFaction(sender);
            if (faction == null) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand", 
                    false
                );
                return;
            }
            if (!faction.isOwner(((Player) sender).getUniqueId())) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.getText("AlertMustBeOwnerToUseCommand"),
                    "AlertMustBeOwnerToUseCommand", 
                    false
                );
                return;
            }
        } else {
            if (!(this.checkPermissions(sender, "mf.unclaimall.others", "mf.admin"))) return;
            faction = this.getFaction(String.join(" ", args));
            if (faction == null) {
                this.playerService.sendMessage(
                    sender, 
                    "&c" + this.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)), 
                    true
                );
                return;
            }
        }
        // remove faction home
        faction.setFactionHome(null);
        this.messageFaction(
            faction, 
            this.translate("&c" + this.getText("AlertFactionHomeRemoved")),
            this.messageService.getLanguage().getString("AlertFactionHomeRemoved")
        );

        // remove claimed chunks
        this.chunkDataAccessor.removeAllClaimedChunks(faction.getName());
        this.dynmapIntegrator.updateClaims();
        this.playerService.sendMessage(
            sender, 
            "&a" + this.getText("AllLandUnclaimedFrom", faction.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AllLandUnclaimedFrom")).replace("#name#", faction.getName()), 
            false
        );

        // remove locks associated with this faction
        this.persistentData.removeAllLocks(faction.getName());
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (! this.checkPermissions(sender)) return null;
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}