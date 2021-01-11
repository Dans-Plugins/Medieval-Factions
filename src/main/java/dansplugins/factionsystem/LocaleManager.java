package dansplugins.factionsystem;

import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Set;

public class LocaleManager {

    private static LocaleManager instance;

    public static final int DEFAULT_BUFFER_SIZE = 8192;

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
            // prepare file object
            String fileName = "en-us.json";
            File file = new File("./plugins/MedievalFactions/" + fileName);

            // get resource as input stream
            InputStream inputStream = MedievalFactions.getInstance().getResource(fileName);

            // TODO: turn input stream into JSON object

            // get keys from json object
//            Set<String> keys = obj.keySet();

            // for each key
//            for (String key : keys) {
//                strings.put(key, (String) obj.get(key)); // place key
//            }

        } catch (Exception e) {
            System.out.println("Error loading from JSON!");
            e.printStackTrace();
        }
    }

    private void saveToJSON() {
        // TODO
    }

}
