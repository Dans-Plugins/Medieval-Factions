/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class JoinCommand extends SubCommand {
    private final Logger logger;

    public JoinCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "join", LOCALE_PREFIX + "CmdJoin"
        }, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.join";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            playerService.sendMessageType(player, "&c" + getText("UsageJoin"), "UsageJoin", false);
            return;
        }
        if (persistentData.isInFaction(player.getUniqueId())) {
            playerService.sendMessageType(player, "&c" + getText("AlertAlreadyInFaction")
                    , "AlertAlreadyInFaction", false);
            return;
        }
        final Faction target = getFaction(String.join(" ", args));
        if (target == null) {
            playerService.sendMessageType(player, "&c" + getText("FactionNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true);
            return;
        }
        if (!target.isInvited(player.getUniqueId())) {
            playerService.sendMessageType(player, "&cYou are not invited to this faction."
                    , "NotInvite", false);
            return;
        }
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            logger.debug("Join event was cancelled.");
            return;
        }
        messageFaction(target, "&a" + getText("HasJoined", player.getName(), target.getName())
                , Objects.requireNonNull(messageService.getLanguage().getString("HasJoined"))
                        .replace("#name#", player.getName())
                        .replace("#faction#", target.getName()));
        target.addMember(player.getUniqueId());
        target.uninvite(player.getUniqueId());
        player.sendMessage(translate("&a" + getText("AlertJoinedFaction")));
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