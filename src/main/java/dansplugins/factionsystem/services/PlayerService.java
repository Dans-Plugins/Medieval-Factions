package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class PlayerService {

    private final MedievalFactions medievalFactions;
    private final MessageService messageService;

    public PlayerService(MedievalFactions medievalFactions, MessageService messageService) {
        this.medievalFactions = medievalFactions;
        this.messageService = messageService;
    }

    public String getMessageType(String oldtype, String newtype) {
        if (medievalFactions.USE_NEW_LANGUAGE_FILE) {
            return newtype;
        } else {
            return oldtype;
        }
    }

    public void sendMessageType(CommandSender sender, String oldtype, String newtype, Boolean replace) {
        if (!replace) {
            sender.sendMessage(colorize(getMessageType(oldtype, messageService.getLanguage().getString(newtype))));
        } else {
            sender.sendMessage(colorize(getMessageType(oldtype, newtype)));
        }
    }

    public void sendListMessage(CommandSender sender, List<String> msg) {
        msg.forEach(s -> sender.sendMessage(colorize(s)));
    }

    public void sendConsoleMessage(ConsoleCommandSender c, String msg, Boolean message) {
        if (!message) {
            c.sendMessage(colorize(msg));
        } else {
            c.sendMessage(colorize(messageService.getLanguage().getString(msg)));
        }
    }

    public void sendConsoleMessage(ConsoleCommandSender c, List<String> msg) {
        msg.forEach(s -> sendConsoleMessage(c, s, false));
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
