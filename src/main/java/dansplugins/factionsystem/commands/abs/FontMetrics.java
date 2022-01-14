/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands.abs;

import org.bukkit.ChatColor;

import java.util.Arrays;

/**
 * Credits to: https://www.spigotmc.org/threads/free-code-sending-perfectly-centered-chat-message.95872/
 * Modified: 07/06/2021 - 10:15
 */
public enum FontMetrics {

    f('f', 4),
    I('I', 3),
    i('i', 1),
    k('k', 4),
    l('l', 1),
    t('t', 4),
    EXCLAMATION_POINT('!', 1),
    AT_SYMBOL('@', 6),
    LEFT_PARENTHESIS('(', 4),
    RIGHT_PARENTHESIS(')', 4),
    LEFT_CURL_BRACE('{', 4),
    RIGHT_CURL_BRACE('}', 4),
    LEFT_BRACKET('[', 3),
    RIGHT_BRACKET(']', 3),
    COLON(':', 1),
    SEMI_COLON(';', 1),
    DOUBLE_QUOTE('"', 3),
    SINGLE_QUOTE('\'', 1),
    LEFT_ARROW('<', 4),
    RIGHT_ARROW('>', 4),
    LINE('|', 1),
    TICK('`', 2),
    PERIOD('.', 1),
    COMMA(',', 1),
    SPACE(' ', 3),
    DEFAULT('a', 4); // Default is 4 pixels, the character doesn't matter here.

    private final static int CENTER_PIXEL = 154;

    private final char character;
    private final int length;

    FontMetrics(char character, int length) {
        this.character = character;
        this.length = length;
    }

    public char getCharacter() {
        return this.character;
    }

    public int getLength() {
        return this.length;
    }

    public int getBoldLength() {
        if (this == FontMetrics.SPACE) return this.getLength(); // Bold Space == Regular Space.
        return this.length + 1;
    }

    public static FontMetrics getMetric(char c) {
        return Arrays.stream(FontMetrics.values())
                .filter(dFI -> dFI.getCharacter() == c)
                .findFirst().orElse(FontMetrics.DEFAULT);
    }

    public static String obtainCenteredMessage(String message) {
        if (message == null || message.equals("")) return message;
        message = ChatColor.translateAlternateColorCodes('&', message);
        int messagePxSize = 0;
        boolean previousCode = false;
        boolean isBold = false;
        for (char c : message.toCharArray()) {
            if (c == '§') {
                previousCode = true;
            } else if (previousCode) {
                previousCode = false;
                isBold = c == 'l' || c == 'L';
            } else {
                FontMetrics dFI = FontMetrics.getMetric(c);
                messagePxSize += isBold ? dFI.getBoldLength() : dFI.getLength();
                messagePxSize++;
            }
        }

        int halvedMessageSize = messagePxSize / 2;
        int toCompensate = CENTER_PIXEL - halvedMessageSize;
        int spaceLength = FontMetrics.SPACE.getLength() + 1;
        int compensated = 0;
        StringBuilder sb = new StringBuilder();
        while (compensated < toCompensate) {
            sb.append(" ");
            compensated += spaceLength;
        }
        return sb + message;
    }

    public static void main(String[] args) {
        final String test = "This is a test, to measure the length of a string";
        int length = 0;
        for (char character : test.toCharArray()) {
            length += FontMetrics.getMetric(character).getLength();
        }
        System.out.println("'" + test + "' is approximately " + length + " minecraft-pixels wide");
    }

}
