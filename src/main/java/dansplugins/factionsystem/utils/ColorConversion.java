package dansplugins.factionsystem.utils;

import org.bukkit.Color;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * @author Callum Johnson
 * @since 23/09/2021 - 22:39
 */
public class ColorConversion {

    /**
     * The attemptDecode method uses multiple parsing methodologies to
     * determine the hex representation of the given input String.
     * <p>
     * Examples of input could be: 'red', 'blue', 'light_blue' etc. etc.<br>
     * Hopeful output could be: '#f00' etc. etc.<br><br>
     * If the value isn't parsed successfully, this method returns <code>null</code>.
     * </p>
     *
     * @param input   string to convert into hexadecimal.
     * @param ideTest to be used by test-cases, Bukkit isn't set to be provided during runtime, or compiled,
     *                which means an error will be thrown if the Class isn't found while testing.
     * @return hexadecimal representation or null.
     * @see Color
     * @see java.awt.Color
     */
    @Nullable
    public static String attemptDecode(@NotNull String input, boolean ideTest) {
        final DefaultColor of = DefaultColor.of(input.trim()); // Medieval Factions List.
        if (of != DefaultColor.NOT_FOUND) return of.getColor();
        Integer[] rgb = obtainRGBFromString(java.awt.Color.class, input); // AWT Graphics List.
        if (!ideTest) if (rgb == null || rgb.length != 3) rgb = obtainRGBFromString(Color.class, input); // Bukkit List.
        if (rgb == null || rgb.length != 3) return null;
        // Return hex String from rgb value if it isn't null and if its 3 in length.
        return "#" + Integer.toHexString(new java.awt.Color(rgb[0], rgb[1], rgb[2]).getRGB()).substring(2);
    }

    /**
     * Method to use reflection to obtain more pre-defined colors for parsing in-game.
     * <p>
     * This method uses reflection to obtain variables to obtain the RGB value for the given input.
     * <br>Example: 'red', is matched to AWT 'RED', so '255, 0, 0' is returned.
     * <br>Example: 'fuchsia' is matched to Bukkit 'FUCHSIA', so '255, 0, 255' is returned.
     * </p>
     *
     * @param colorClass to scan and access with reflection.
     * @param input      string to parse to the color.
     * @return {@link Integer} array or <code>null</code>.
     * @see Color
     * @see java.awt.Color
     */
    @Nullable
    private static Integer[] obtainRGBFromString(@NotNull Class<?> colorClass, @NotNull String input) {
        try {
            // The following methods are present in both Awt and Bukkit Color classes.
            final Method getRed = colorClass.getMethod("getRed");
            final Method getGreen = colorClass.getMethod("getGreen");
            final Method getBlue = colorClass.getMethod("getBlue");
            final Field[] fields = colorClass.getFields();
            for (Field field : fields) {
                // is the field a predefined color?
                if (field.getType().equals(colorClass)) {
                    if (field.getName().equalsIgnoreCase(input)) {
                        // Static fields can be obtained without an object. (null)
                        final Object color = field.get(null);
                        final int red = (int) getRed.invoke(color);
                        final int green = (int) getGreen.invoke(color);
                        final int blue = (int) getBlue.invoke(color);
                        return new Integer[]{red, green, blue};
                    }
                }
            }
            // No field matched the input, default to the try/catch and return null.
            throw new ReflectiveOperationException("Failed to find Color for this class to match '" + input + "'!");
        } catch (ReflectiveOperationException ex) {
            return null;
        }
    }

    /**
     * Default Color enumeration stands for a list of MedievalFaction specific predefined
     * colors for usage with the Dynmap Territory Color flag.
     */
    private enum DefaultColor {

        // https://htmlcolors.com/color-names
        BLACK("#000000"), // Black
        DARK_BLUE("#151B8D"), // Denim Dark Blue
        DARK_GREEN("#254117"), // Dark Forest Green
        DARK_AQUA("#348781"), // Medium Aquamarine
        DARK_RED("#990012"), // Red Wine
        DARK_PURPLE("#461B7E"), // Purple Monster
        DARK_GRAY("#736F6E"), // Gray
        GOLD("#FDD017"), // Bright Gold
        GRAY("#B6B6B4"), // Gray Cloud
        BLUE("#1569C7"), // Blue Eyes
        GREEN("#41A317"), // Lime Green
        AQUA("#00FFFF"), // Cyan or Aqua
        RED("#FF0000"), // Red
        PURPLE("#FF00FF"), // Magenta
        YELLOW("#FFFF00"), // Yellow
        WHITE("#FFFFFF"), // White

        NOT_FOUND("null"); // Default.

        private final String color;

        DefaultColor(String color) {
            this.color = color;
        }

        public String getColor() {
            return color;
        }

        public boolean equals(String in) {
            return name().equalsIgnoreCase(in) ||
                    name().equalsIgnoreCase(in.replaceAll(" ", "_")) ||
                    getColor().equalsIgnoreCase(in) ||
                    getColor().equalsIgnoreCase(in.replaceAll(" ", "_"));
        }

        @Override
        public String toString() {
            return getColor();
        }

        public static DefaultColor of(String in) {
            for (DefaultColor out : DefaultColor.values()) if (out.equals(in)) return out;
            return DefaultColor.NOT_FOUND;
        }

    }

    public static void main(String[] args) {
        System.out.println(attemptDecode("red", true)); // #FF0000 (Default Color)
        System.out.println(attemptDecode("blue", true)); // #1569C7 (Default Color)
        System.out.println(attemptDecode("purple", true)); // #FF00FF (Default Color)
        System.out.println(attemptDecode("teal", true)); // null (Bukkit Color)
        System.out.println(attemptDecode("black", true)); // #000000 (Default Color)
        System.out.println(attemptDecode("orange", true)); // #ffc800 (AWT Color)
        System.out.println(attemptDecode("gray", true)); // #B6B6B4 (Default Color)
    }

}
