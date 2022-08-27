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
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class ConfigCommand extends SubCommand {
    private final MedievalFactions medievalFactions;

    public ConfigCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "config", LOCALE_PREFIX + "CmdConfig"
        }, false, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
        this.medievalFactions = medievalFactions;
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
        if (!(checkPermissions(sender, "mf.config", "mf.admin"))) {
            return;
        }

        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Valid subcommands: show, set, reload");
            return;
        }

        final boolean show = safeEquals(args[0], "get", "show", getText("CmdConfigShow"));
        final boolean set = safeEquals(args[0], "set", getText("CmdConfigSet"));
        final boolean reload = safeEquals(args[0], "reload", "CmdConfigReload");

        if (show) {
            if (args.length < 2) {
                sender.sendMessage(translate("&c" + getText("UsageConfigShow")));
                return;
            }

            int page = getIntSafe(args[1], -1);

            if (page == -1) {
                sender.sendMessage(translate("&c" + getText("ArgumentMustBeNumber")));
                return;
            }

            switch (page) {
                case 1:
                    configService.sendPageOneOfConfigList(sender);
                    break;
                case 2:
                    configService.sendPageTwoOfConfigList(sender);
                    break;
                default:
                    sender.sendMessage(translate("&c" + getText("UsageConfigShow")));
            }
        } else if (set) {
            if (args.length < 3) {
                sender.sendMessage(translate("&c" + getText("UsageConfigSet")));
            } else {
                configService.setConfigOption(args[1], args[2], sender);
            }
        } else if (reload) {
            medievalFactions.reloadConfig();
            messageService.reloadLanguage();
            sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
        } else {
            sender.sendMessage(translate("&c" + getText("ValidSubCommandsShowSet")));
        }
    }
}