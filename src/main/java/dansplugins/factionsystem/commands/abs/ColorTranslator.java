/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands.abs;

import org.bukkit.ChatColor;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author Callum Johnson
 * @since 05/05/2021 - 12:38
 */
public interface ColorTranslator {

    /**
     * Method to translate color codes from & to MC Equivalents.
     * @param message to translate.
     * @return translated message.
     */
    default String translate(String message) {
        return ChatColor.translateAlternateColorCodes('&', message);
    }

    /**
     * Method to translate color codes from & to MC Equivalents.
     * @param messages to translate.
     * @return translated messages.
     */
    default List<String> translate(List<String> messages) {
        return messages.stream().map(this::translate).collect(Collectors.toList());
    }

}
