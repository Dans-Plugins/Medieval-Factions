package dansplugins.factionsystem;

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
        switch(color) {
            case "aqua":
            case "AQUA":
                return ChatColor.AQUA;
            case "black":
            case "BLACK":
                return ChatColor.BLACK;
            case "blue":
            case "BLUE":
                return ChatColor.BLUE;
            case "dark_aqua":
            case "DARK_AQUA":
                return ChatColor.DARK_AQUA;
            case "dark_blue":
            case "DARK_BLUE":
                return ChatColor.DARK_BLUE;
            case "dark_gray":
            case "DARK_GRAY":
                return ChatColor.DARK_GRAY;
            case "dark_green":
            case "DARK_GREEN":
                return ChatColor.DARK_GREEN;
            case "dark_purple":
            case "DARK_PURPLE":
                return ChatColor.DARK_PURPLE;
            case "dark_red":
            case "DARK_RED":
                return ChatColor.DARK_RED;
            case "gold":
            case "GOLD":
                return ChatColor.GOLD;
            case "gray":
            case "GRAY":
                return ChatColor.GRAY;
            case "green":
            case "GREEN":
                return ChatColor.GREEN;
            case "light_purple":
            case "LIGHT_PURPLE":
                return ChatColor.LIGHT_PURPLE;
            case "red":
            case "RED":
                return ChatColor.RED;
            case "YELLOW":
            case "yellow":
                return ChatColor.YELLOW;
            case "WHITE":
            case "white":
            default:
                return ChatColor.WHITE;
        }
    }

}
