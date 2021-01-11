package dansplugins.factionsystem;

import dansplugins.factionsystem.utils.Pair;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Scanner;

public class LocaleManager {

    private static LocaleManager instance;

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

    public String getText(String key) {
        return strings.get(key);
    }

    public void loadStrings() {
        if (isFilePresent("./plugins/MedievalFactions/en-us.tsv")) {
            loadFromPluginFolder();
            System.out.println("Loading from plugin folder!");
        }
        else {
            loadFromResource();
            System.out.println("Loading from resource!");
        }
    }

    private boolean isFilePresent(String path) {
        File file = new File(path);
        return file.exists();
    }

    private void loadFromPluginFolder() {
        File file = new File("./plugins/MedievalFactions/en-us.tsv");
        try {
            loadFromFile(file);

            if (MedievalFactions.getInstance().isVersionMismatched()) {
                handleVersionMismatch();
            }

        } catch (Exception e) {
            System.out.println("Something went wrong loading from the plugin folder.");
            e.printStackTrace();
        }

    }

    private void loadFromFile(File file) {
        try {
            Scanner reader = new Scanner(file);

            while (reader.hasNextLine()) {
                String line = reader.nextLine();
                Pair<String, String> pair = getPairFromLine(line);
                strings.put(pair.getLeft(), pair.getRight());
                keys.add(pair.getLeft());
            }

            reader.close();

        } catch (Exception e) {
            System.out.println("Something went wrong loading from file!");
            e.printStackTrace();
        }

    }

    // this should be called after loading from plugin folder
    private void handleVersionMismatch() {
        System.out.println("Version mismatch! Ensuring all localization keys are found!");

        String fileName = "en-us.tsv";

        // get resource as input stream
        InputStream inputStream = MedievalFactions.getInstance().getResource(fileName);

        InputStreamReader reader = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(reader);
        br.lines().forEach(line -> {
            Pair<String, String> pair = getPairFromLine(line);

            if (!strings.containsKey(pair.getLeft())) {
                strings.put(pair.getLeft(), pair.getRight());
                keys.add(pair.getLeft());
            }

        });

    }

    private void loadFromResource() {
        try {
            String fileName = "en-us.tsv";

            // get resource as input stream
            InputStream inputStream = MedievalFactions.getInstance().getResource(fileName);

            loadFromInputStream(inputStream);

            saveToPluginFolder();

        } catch (Exception e) {
            System.out.println("Error loading from JSON!");
            e.printStackTrace();
        }
    }

    private void loadFromInputStream(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream);

        BufferedReader br = new BufferedReader(reader);
        br.lines().forEach(line -> {
            Pair<String, String> pair = getPairFromLine(line);
            strings.put(pair.getLeft(), pair.getRight());
            keys.add(pair.getLeft());
        });
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
                writer.write(key + "\t" + strings.get(key) + "\n");
            }

        } catch (Exception e) {
            System.out.println("There was a problem saving the strings.");
            e.printStackTrace();
        }
    }

}
