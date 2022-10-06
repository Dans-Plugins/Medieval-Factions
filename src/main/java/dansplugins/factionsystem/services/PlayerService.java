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
     * @param tsvMessage The old type of message.
     * @param ymlMessage The new type of message.
     * @return
     */
    public String decideWhichMessageToUse(String tsvMessage, String ymlMessage) {
        if (configService.getBoolean("useNewLanguageFile")) {
            return ymlMessage;
        }
        else {
            return tsvMessage;
        }
    }

    /**
     * Send a message to the player.
     * @param sender The player to send the message to.
     * @param tsvMessage The old type of message.
     * @param ymlMessage The new type of message.
     * @param placeholdersReplaced Whether or not parts of the message in the newtype are replaced.
     */
    public void sendMessage(CommandSender sender, String tsvMessage, String ymlMessage, Boolean placeholdersReplaced) {
        if (!placeholdersReplaced) {
            sender.sendMessage(colorize(decideWhichMessageToUse(tsvMessage, messageService.getLanguage().getString(ymlMessage))));
        }
        else {
            sender.sendMessage(colorize(decideWhichMessageToUse(tsvMessage, ymlMessage)));
        }
    }

    /**
     * Send multiple messages to the player.
     * @param sender The player to send the messages to.
     * @param messages The messages to send.
     */
    public void sendMultipleMessages(CommandSender sender, List<String> messages) {
        messages.forEach(s -> sender.sendMessage(colorize(s)));
    }

    /**
     * Send a message to the console.
     * @param console The console to send the message to.
     * @param message The message to send.
     * @param useMessageService Whether or not to use the message service.
     */
    public void sendMessageToConsole(ConsoleCommandSender console, String message, Boolean useMessageService) {
        if (!useMessageService) {
            console.sendMessage(colorize(message));
        }
        else {
            console.sendMessage(colorize(messageService.getLanguage().getString(message)));
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
