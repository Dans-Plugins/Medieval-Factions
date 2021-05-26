package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ChatCommand extends SubCommand {

    public ChatCommand() {
        super(new String[] {
              "chat", LOCALE_PREFIX + "CmdChat"
        }, true, true);
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
        final boolean contains = ephemeral.getPlayersInFactionChat().contains(player.getUniqueId());
        final String path = (contains ? "NoLonger" : "NowSpeaking") + "InFactionChat";
        if (contains) ephemeral.getPlayersInFactionChat().remove(player.getUniqueId());
        else ephemeral.getPlayersInFactionChat().add(player.getUniqueId());
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
