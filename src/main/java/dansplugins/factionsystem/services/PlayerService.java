package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

public class PlayerService {
    private final ConfigService configService;
    private final MessageService messageService;

    public PlayerService(ConfigService configService, MessageService messageService) {
        this.configService = configService;
        this.messageService = messageService;
    }

    public String decideWhichMessageToUse(String oldtype, String newtype) {
        if (configService.getBoolean("useNewLanguageFile")) {
            return newtype;
        } else {
            return oldtype;
        }
    }

    public void sendMessage(CommandSender sender, String oldtype, String newtype, Boolean replace) {
        if (!replace) {
            sender.sendMessage(colorize(decideWhichMessageToUse(oldtype, messageService.getLanguage().getString(newtype))));
        } else {
            sender.sendMessage(colorize(decideWhichMessageToUse(oldtype, newtype)));
        }
    }

    public void sendMultipleMessages(CommandSender sender, List<String> msg) {
        msg.forEach(s -> sender.sendMessage(colorize(s)));
    }

    public void sendMessageToConsole(ConsoleCommandSender c, String msg, Boolean message) {
        if (!message) {
            c.sendMessage(colorize(msg));
        } else {
            c.sendMessage(colorize(messageService.getLanguage().getString(msg)));
        }
    }

    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
