package dansplugins.factionsystem;

import dansplugins.factionsystem.utils.Pair;

import java.io.*;
import java.util.*;

public class LocaleManager {

    private static LocaleManager instance;

    private ArrayList<String> supportedLanguageIDs = new ArrayList<>(Arrays.asList("en-us"));

    private ArrayList<String> keys = new ArrayList<>();
    private HashMap<String, String> strings = new HashMap<>();

    private final String pluginFolderPath = "./plugins/MedievalFactions/";
    private final String languageFolderPath = pluginFolderPath + "languages/";
    private final String localizationFileName = MedievalFactions.getInstance().getConfig().getString("languageid") + ".tsv";
    private final String localizationFilePath = languageFolderPath + localizationFileName;

    private LocaleManager() {

    }

    public static LocaleManager getInstance() {
        if (instance == null) {
            instance = new LocaleManager();
        }
        return instance;
    }

    public String getText(String key) {
        if (!keys.contains(key)) {
            System.out.println("ERROR -> Key not found: " + key);
            return "[key not found]";
        }
        return strings.get(key);
    }

    public void loadStrings() {
        if (isFilePresent(localizationFilePath)) {
            loadFromPluginFolder();
            System.out.println("DEBUG: Loading from plugin folder!");
        }
        else {
            loadFromResource();
            System.out.println("DEBUG: Loading from resource!");
        }
        System.out.println(getText("KeysLoaded") + keys.size());
    }

    public boolean isLanguageIDSupported(String ID) {
        return supportedLanguageIDs.contains(ID);
    }

    private boolean isFilePresent(String path) {
        File file = new File(path);
        return file.exists();
    }

    private void loadFromPluginFolder() {
        File file = new File(localizationFilePath);
        try {
            loadFromFile(file);

            if (MedievalFactions.getInstance().isVersionMismatched()) {
                handleVersionMismatch();
            }

        } catch (Exception e) {
            System.out.println("DEBUG: Something went wrong loading from the plugin folder.");
            e.printStackTrace();
        }

    }

    private void loadFromFile(File file) {
        try {
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                Pair<String, String> pair = getPairFromLine(line);
                if (pair != null && !strings.containsKey(pair.getLeft())) {
                    strings.put(pair.getLeft(), pair.getRight());
                    keys.add(pair.getLeft());
                }
            }

            reader.close();

        } catch (Exception e) {
            System.out.println("DEBUG: Something went wrong loading from file!");
            e.printStackTrace();
        }

    }

    // this should be called after loading from plugin folder
    private void handleVersionMismatch() {
        // get en-us resource as input stream
        InputStream inputStream = MedievalFactions.getInstance().getResource(languageFolderPath + "en-us.tsv");

        loadFromInputStream(inputStream); // load in any missing keys

        saveToPluginFolder();
    }

    private void loadFromResource() {
        try {
            // get resource as input stream
            InputStream inputStream = MedievalFactions.getInstance().getResource(localizationFileName);

            loadFromInputStream(inputStream);

            saveToPluginFolder();

        } catch (Exception e) {
            System.out.println("DEBUG: Error loading from resource!");
            e.printStackTrace();
        }
    }

    private void loadFromInputStream(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream);
        BufferedReader br = new BufferedReader(reader);
        br.lines().forEach(line -> {
            Pair<String, String> pair = getPairFromLine(line);
            if (pair != null && !strings.containsKey(pair.getLeft())) { // if pair found and if key not already loaded
                strings.put(pair.getLeft(), pair.getRight());
                keys.add(pair.getLeft());
            }
        });
    }

    private Pair<String, String> getPairFromLine(String line) {
        String key = "";
        String value = "";

        int tabIndex = getIndexOfTab(line);

        if (tabIndex != -1) {
            key = line.substring(0, tabIndex);
            value = line.substring(tabIndex + 1);
            return new Pair<>(key, value);
        }
        else {
            return null;
        }

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

        sortKeys();

        try {
            File folder = new File(languageFolderPath);

            if (!folder.exists()) {
                folder.mkdir();
            }

            File file = new File(localizationFilePath);

            file.createNewFile();

            FileWriter writer = new FileWriter(file);

            for (String key : keys) {
                writer.write(key + "\t" + strings.get(key) + "\n");
            }

        } catch (Exception e) {
            System.out.println("DEBUG: There was a problem saving the strings.");
            e.printStackTrace();
        }
    }

    private void sortKeys() {
        Collections.sort(keys);
    }

}
