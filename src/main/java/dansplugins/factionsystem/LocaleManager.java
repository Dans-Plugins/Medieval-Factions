package dansplugins.factionsystem;

import org.bukkit.plugin.java.JavaPlugin;
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
            String path = "en-us.json";
            File file = new File("./plugins/MedievalFactions/" + path);
            InputStream inputStream = MedievalFactions.getInstance().getResource(path);
            copyInputStreamToFile(inputStream, file);
            FileReader reader = new FileReader(file);
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

    // source: https://mkyong.com/java/how-to-convert-inputstream-to-file-in-java/
    private void copyInputStreamToFile(InputStream inputStream, File file) throws IOException {

        // append = false
        try (FileOutputStream outputStream = new FileOutputStream(file, false)) {
            int read;
            byte[] bytes = new byte[DEFAULT_BUFFER_SIZE];
            while ((read = inputStream.read(bytes)) != -1) {
                outputStream.write(bytes, 0, read);
            }
        }

    }

    private void saveToJSON() {
        // TODO
    }

}
