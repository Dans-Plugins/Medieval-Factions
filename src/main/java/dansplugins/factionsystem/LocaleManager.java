package dansplugins.factionsystem;

import java.util.HashMap;

public class LocaleManager {

    private static LocaleManager instance;

    private HashMap<String, String> strings = new HashMap<>();

    private LocaleManager() {

    }

    public static LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    public void loadStrings() {
        loadFromJSON();
    }

    public void saveStrings() {
        saveToJSON();
    }

    public String getText(String key) {
        return strings.get(key);
    }

    private void loadFromJSON() {
        // TODO
    }

    private void saveToJSON() {
        // TODO
    }



}
