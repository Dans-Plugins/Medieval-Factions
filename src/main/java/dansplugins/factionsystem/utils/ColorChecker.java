package dansplugins.factionsystem.utils;

import org.bukkit.ChatColor;

public class ColorChecker {

    private static ColorChecker instance;

    private ColorChecker() {

    }

    public static ColorChecker getInstance() {
        if (instance == null) {
            instance = new ColorChecker();
        }
        return instance;
    }

    public ChatColor getColorByName(String color) {
        for (ChatColor value : ChatColor.values()) {
            if (value.name().equalsIgnoreCase(color)) {
                return value;
            }
        }
        return ChatColor.WHITE;
    }

}
