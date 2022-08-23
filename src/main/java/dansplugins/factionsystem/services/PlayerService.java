package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.command.ConsoleCommandSender;
import org.bukkit.entity.Player;

import java.util.List;

public class PlayerService {
    public static void sendPlayerMessage(Player p, String... msg) {
        for (String string : msg) {
            p.sendMessage(colorize(string));
        }
    }

    public static void sendPlayerMessage(Player p, List<String> msg) {
        for (String string : msg) {
            p.sendMessage(colorize(string));
        }
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, String... msg) {
        for (String string : msg) {
            c.sendMessage(colorize(string));
        }
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, List<String> msg) {
        for (String string : msg) {
            c.sendMessage(colorize(string));
        }
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
