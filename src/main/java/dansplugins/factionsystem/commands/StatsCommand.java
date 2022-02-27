/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;

/**
 * @author Callum Johnson
 */
public class StatsCommand extends SubCommand {
    public StatsCommand() {
        super(new String[]{
                "stats", LOCALE_PREFIX + "CmdStats"
        }, false, false, false, false);
    }

    @Override
    public void execute(Player player, String[] args, String key) {
        execute((CommandSender) player, args, key);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        sender.sendMessage(ChatColor.AQUA + "=== Medieval Factions Stats ===");
        sender.sendMessage(ChatColor.AQUA + "Number of factions: " + PersistentData.getInstance().getNumFactions());
    }
}