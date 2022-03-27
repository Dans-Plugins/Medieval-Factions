package dansplugins.factionsystem.utils;

import java.util.HashMap;

import dansplugins.factionsystem.services.LocalLocaleService;

/**
 * @author Daniel McCoy Stephenson
 */
public class Locale {

    private static HashMap<String, Integer> keyUses = new HashMap<>();
    
    public static String get(String key) {
        recordKeyUse(key);
        return LocalLocaleService.getInstance().getText(key);
    }

    static void recordKeyUse(String key) {
        if (keyUses.containsKey(key)) {
            int numUses = keyUses.get(key);
            keyUses.replace(key, numUses + 1);
        }
        else {
            keyUses.put(key, 1);
        }
        Logger.getInstance().debug("The '" + key + "' key has been used " + keyUses.get(key) + " times since the server was started.");
    }
}
