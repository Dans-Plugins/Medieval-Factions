package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class PlayerService {

    public static String getMessageType(String OldType, String NewType) {
        if (MedievalFactions.USE_NEW_LANGUAGE_FILE) {
            return NewType;
        } else {
            return OldType;
        }
    }

    public static void sendMessageType(CommandSender sender, String OldType, String NewType, Boolean replace) {
        if (!replace) {
            sender.sendMessage(colorize(getMessageType(OldType, MessageService.getLanguage().getString(NewType))));
        } else {
            sender.sendMessage(colorize(getMessageType(OldType, NewType)));
        }
    }

    public static void sendListMessage(CommandSender sender, List<String> msg) {
        msg.forEach(s -> sender.sendMessage(colorize(s)));
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, String msg, Boolean message) {
        if (!message) {
            c.sendMessage(colorize(msg));
        } else {
            c.sendMessage(colorize(MessageService.getLanguage().getString(msg)));
        }
    }

    public static void sendConsoleMessage(ConsoleCommandSender c, List<String> msg) {
        msg.forEach(s -> sendConsoleMessage(c, s, false));
    }

    public static String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
