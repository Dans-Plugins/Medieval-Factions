package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.services.LocalLocaleService;

/**
 * @author Daniel McCoy Stephenson
 */
public class Locale {
    
    public static String get(String key) {
        return LocalLocaleService.getInstance().getText(key);
    }
}
