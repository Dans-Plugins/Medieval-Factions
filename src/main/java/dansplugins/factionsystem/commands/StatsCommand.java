package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class StatsCommand extends SubCommand {
    public StatsCommand() {
        super(new String[] {
                "ally", LOCALE_PREFIX + "CmdAlly"
        }, false, false, false, false);
    }

    @Override
    public void execute(Player player, String[] args, String key) {
        execute(player, args, key);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        // TODO: implement
    }
}