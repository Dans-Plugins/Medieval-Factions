package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.ConsoleCommandSender;

import java.util.List;

/**
 * Sends messages to players and the console.
 */
public class PlayerService {
    private final ConfigService configService;
    private final MessageService messageService;

    /**
     * Constructor for the PlayerService class.
     * @param configService The ConfigService instance.
     * @param messageService The MessageService instance.
     */
    public PlayerService(ConfigService configService, MessageService messageService) {
        this.configService = configService;
        this.messageService = messageService;
    }

    /**
     * Decide which message to send to the player.
     * @param oldtype The old type of message.
     * @param newtype The new type of message.
     * @return
     */
    public String decideWhichMessageToUse(String oldtype, String newtype) {
        if (configService.getBoolean("useNewLanguageFile")) {
            return newtype;
        } else {
            return oldtype;
        }
    }

    /**
     * Send a message to the player.
     * @param sender The player to send the message to.
     * @param oldtype The old type of message.
     * @param newtype The new type of message.
     * @param replace Whether or not parts of the message in the newtype are replaced.
     */
    public void sendMessage(CommandSender sender, String oldtype, String newtype, Boolean replace) {
        if (!replace) {
            sender.sendMessage(colorize(decideWhichMessageToUse(oldtype, messageService.getLanguage().getString(newtype))));
        } else {
            sender.sendMessage(colorize(decideWhichMessageToUse(oldtype, newtype)));
        }
    }

    /**
     * Send multiple messages to the player.
     * @param sender The player to send the messages to.
     * @param msg The messages to send.
     */
    public void sendMultipleMessages(CommandSender sender, List<String> msg) {
        msg.forEach(s -> sender.sendMessage(colorize(s)));
    }

    /**
     * Send a message to the console.
     * @param c The console to send the message to.
     * @param msg The message to send.
     * @param message Whether or not to use the message service.
     */
    public void sendMessageToConsole(ConsoleCommandSender c, String msg, Boolean message) {
        if (!message) {
            c.sendMessage(colorize(msg));
        } else {
            c.sendMessage(colorize(messageService.getLanguage().getString(msg)));
        }
    }

    /**
     * Add color to a string.
     * @param input The string to add color to.
     * @return The string with color.
     */
    public String colorize(String input) {
        return ChatColor.translateAlternateColorCodes('&', input);
    }
}
