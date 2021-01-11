package dansplugins.factionsystem;

import dansplugins.factionsystem.utils.Pair;
import org.bukkit.plugin.java.JavaPlugin;
import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;

import java.io.*;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Set;

public class LocaleManager {

    private static LocaleManager instance;

    public static final int DEFAULT_BUFFER_SIZE = 8192;

    private ArrayList<String> keys = new ArrayList<>();
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
        if (isFilePresent("./plugins/MedievalFactions/en-us.tsv")) {
            loadFromPluginFolder();
        }
        else {
            loadFromResource();
        }
    }

    private boolean isFilePresent(String path) {
        File file = new File(path);
        return file.exists();
    }

    private void loadFromPluginFolder() {
        // TODO
    }

    public String getText(String key) {
        return strings.get(key);
    }

    private void loadFromResource() {
        try {
            // prepare file object
            String fileName = "en-us.tsv";

            // get resource as input stream
            InputStream inputStream = MedievalFactions.getInstance().getResource(fileName);

            InputStreamReader reader = new InputStreamReader(inputStream);

            BufferedReader br = new BufferedReader(reader);
            br.lines().forEach(line -> {
                Pair<String, String> pair = getPairFromLine(line);
                strings.put(pair.getLeft(), pair.getRight());
                keys.add(pair.getLeft());
            });

            saveToPluginFolder();

        } catch (Exception e) {
            System.out.println("Error loading from JSON!");
            e.printStackTrace();
        }
    }

    private Pair<String, String> getPairFromLine(String line) {
        String key = "";
        String value = "";

        int tabIndex = getIndexOfTab(line);
        key = line.substring(0, tabIndex);
        value = line.substring(tabIndex + 1);

        return new Pair<>(key, value);
    }

    private int getIndexOfTab(String line) {
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == '\t') {
                return i;
            }
        }
        return -1;
    }

    private void saveToPluginFolder() {
        File file = new File("./plugins/MedievalFactions/en-us.tsv");
        try {
            file.createNewFile();

            FileWriter writer = new FileWriter(file);

            for (String key : keys) {
                writer.write(key + "\t" + strings.get(key));
            }

        } catch (Exception e) {
            System.out.println("There was a problem saving the strings.");
            e.printStackTrace();
        }
    }

}
