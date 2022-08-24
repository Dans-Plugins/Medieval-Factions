package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class PlayerService {

    public String getMessageType(String OldType, String NewType) {
        if (new MedievalFactions().USE_NEW_LANGUAGE_FILE) {
            return NewType;
        } else {
            return OldType;
        }
    }

    public void sendMessageType(CommandSender sender, String OldType, String NewType, Boolean replace) {
        if (!replace) {
            sender.sendMessage(colorize(getMessageType(OldType, new MessageService().getLanguage().getString(NewType))));
        } else {
            sender.sendMessage(colorize(getMessageType(OldType, NewType)));
        }
    }

    public void sendListMessage(CommandSender sender, List<String> msg) {
        msg.forEach(s -> sender.sendMessage(colorize(s)));
    }

    public void sendConsoleMessage(ConsoleCommandSender c, String msg, Boolean message) {
        if (!message) {
            c.sendMessage(colorize(msg));
        } else {
            c.sendMessage(colorize(new MessageService().getLanguage().getString(msg)));
        }
    }

    public void sendConsoleMessage(ConsoleCommandSender c, List<String> msg) {
        msg.forEach(s -> sendConsoleMessage(c, s, false));
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
