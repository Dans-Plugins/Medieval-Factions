package dansplugins.factionsystem;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.File;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Set;

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
        JSONParser parser = new JSONParser();
        try {
            String path = "en-us.json";
            InputStream stream = MedievalFactions.getInstance().getResource(path);
            InputStreamReader reader = new InputStreamReader(stream);
            JSONObject obj = (JSONObject) parser.parse(reader);

            Set<String> keys = obj.keySet();

            for (String key : keys) {
                strings.put(key, (String) obj.get(key));
            }

        } catch (Exception e) {
            System.out.println("Error loading from JSON!");
            e.printStackTrace();
        }
    }

    private void saveToJSON() {
        // TODO
    }

}
