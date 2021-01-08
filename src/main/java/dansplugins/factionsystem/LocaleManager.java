package dansplugins.factionsystem;

import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.FileReader;
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
            String path = "./plugins/MedievalFactions/en-us.json";
            FileReader reader = new FileReader(path);
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
