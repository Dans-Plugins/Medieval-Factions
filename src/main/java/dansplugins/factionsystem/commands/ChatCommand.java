/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;

/**
 * @author Callum Johnson
 */
public class ChatCommand extends SubCommand {

    public ChatCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "chat", LOCALE_PREFIX + "CmdChat"
        }, true, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.chat";
        if (!player.hasPermission(permission)) {
            player.sendMessage(translate("&c" + getText("PermissionNeeded", permission)));
            return;
        }

        final boolean contains = ephemeralData.getPlayersInFactionChat().contains(player.getUniqueId());

        final String path = (contains ? "NoLonger" : "NowSpeaking") + "InFactionChat";

        if (contains) {
            ephemeralData.getPlayersInFactionChat().remove(player.getUniqueId());
        } else {
            ephemeralData.getPlayersInFactionChat().add(player.getUniqueId());
        }

        player.sendMessage(translate("&a" + getText(path)));
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