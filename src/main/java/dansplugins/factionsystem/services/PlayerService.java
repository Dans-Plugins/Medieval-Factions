package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerService {
    public static void sendPlayerMessage(Player p, String msg, Boolean message) {
        if (!message) {
            p.sendMessage(colorize(msg));
        } else {
            p.sendMessage(colorize(MessageService.getLanguage().getString(msg)));
        }
    }

    public static void sendPlayerMessage(Player p, List<String> msg) {
        msg.forEach(s -> sendPlayerMessage(p, s, false));
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, String msg) {
        c.sendMessage(colorize(msg));
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, List<String> msg) {
        msg.forEach(s -> sendConsoleMessage(c, msg));
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
