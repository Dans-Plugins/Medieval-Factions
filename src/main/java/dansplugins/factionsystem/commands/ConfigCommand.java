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
import dansplugins.factionsystem.utils.TabCompleteTools;
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
        }, false, ["mf.config", "mf.admin"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length == 0) {
            sender.sendMessage(ChatColor.RED + "Valid subcommands: show, set, reload");
            return;
        }

        final boolean show = this.safeEquals(args[0], "get", "show", this.getText("CmdConfigShow"));
        final boolean set = this.safeEquals(args[0], "set", this.getText("CmdConfigSet"));
        final boolean reload = this.safeEquals(args[0], "reload", "CmdConfigReload");

        if (show) {
            if (args.length < 2) {
                sender.sendMessage(this.translate("&c" + this.getText("UsageConfigShow")));
                return;
            }

            int page = this.getIntSafe(args[1], -1);

            if (page == -1) {
                sender.sendMessage(this.translate("&c" + this.getText("ArgumentMustBeNumber")));
                return;
            }

            switch (page) {
                case 1:
                    this.configService.sendPageOneOfConfigList(sender);
                    break;
                case 2:
                    this.configService.sendPageTwoOfConfigList(sender);
                    break;
                default:
                    sender.sendMessage(this.translate("&c" + this.getText("UsageConfigShow")));
            }
        } else if (set) {
            if (args.length < 3) {
                sender.sendMessage(this.translate("&c" + this.getText("UsageConfigSet")));
            } else {
                this.configService.setConfigOption(args[1], args[2], sender);
            }
        } else if (reload) {
            this.medievalFactions.reloadConfig();
            this.messageService.reloadLanguage();
            sender.sendMessage(ChatColor.GREEN + "Config reloaded.");
        } else {
            sender.sendMessage(this.translate("&c" + this.getText("ValidSubCommandsShowSet")));
        }
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "show", "set", "reload");
        } else if (args.length == 2) {
            if (args[0] == "show") return TabCompleteTools.completeMultipleOptions(args[1], "1", "2");
            if (args[0] == "set") return TabCompleteTools.filterStartingWith(args[1], configService.getStringConfigOptions());
        }
        return null;
    }
}