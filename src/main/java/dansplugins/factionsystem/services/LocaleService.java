/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import preponderous.ponder.misc.Pair;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;

/**
 * @author Daniel McCoy Stephenson
 */
public class LocaleService {
    private final MedievalFactions medievalFactions;
    private final ConfigService configService;

    private final ArrayList<String> supportedLanguageIDs = new ArrayList<>(Arrays.asList("en-us", "es", "ru", "pt-br", "de"));

    private final ArrayList<String> keys = new ArrayList<>();
    private final HashMap<String, String> strings = new HashMap<>();

    private String languageFolderPath;
    private String localizationFileName;
    private String localizationFilePath;

    public LocaleService(MedievalFactions medievalFactions, ConfigService configService) {
        this.medievalFactions = medievalFactions;
        this.configService = configService;
        initializePaths();
    }

    private void initializePaths() {
        String pluginFolderPath = "./plugins/MedievalFactions/";
        languageFolderPath = pluginFolderPath + "languages/";
        localizationFileName = configService.getString("languageid") + ".tsv";
        localizationFilePath = languageFolderPath + localizationFileName;
    }

    public String get(String key) {
        return getText(key);
    }

    public String getText(String key) {
        if (!keys.contains(key)) return String.format("[key '%s' not found]", key);
        return strings.get(key);
    }

    public void loadStrings() {

        if (isFilePresent(localizationFilePath)) {
            loadFromPluginFolder();
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Loaded from plugin folder!");
            }
        } else {
            loadFromResource();
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Loaded from resource!");
            }
        }
        if (medievalFactions.isDebugEnabled()) {
            System.out.printf((getText("KeysLoaded")) + "%n", keys.size());
        }
    }

    public void reloadStrings() {
        initializePaths();
        keys.clear();
        strings.clear();
        loadStrings();
    }

    public boolean isLanguageIDSupported(String ID) {
        return supportedLanguageIDs.contains(ID);
    }

    public String getSupportedLanguageIDsSeparatedByCommas() {

        StringBuilder IDs = new StringBuilder();
        for (int i = 0; i < supportedLanguageIDs.size(); i++) {
            IDs.append(supportedLanguageIDs.get(i));
            if (i != supportedLanguageIDs.size() - 1) {
                IDs.append(", ");
            }
        }
        return IDs.toString();

    }

    private boolean isFilePresent(String path) {
        File file = new File(path);
        return file.exists();
    }

    private void loadFromPluginFolder() {
        File file = new File(localizationFilePath);
        try {
            loadFromFile(file);
            updateCurrentLocalLanguageFile();
            saveToPluginFolder();

        } catch (Exception e) {
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Something went wrong loading from the plugin folder.");
            }
            e.printStackTrace();
        }

    }

    private void loadFromFile(File file) {
        try {
            List<String> lines = Files.readAllLines(file.toPath());

            for (String line : lines) {
                Pair<String, String> pair = getPairFromLine(line);
                if (pair != null && !strings.containsKey(pair.getLeft())) {
                    strings.put(pair.getLeft(), pair.getRight());
                    keys.add(pair.getLeft());
                }
            }

        } catch (Exception e) {
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Something went wrong loading from file!");
            }
            e.printStackTrace();
        }

    }

    // this should be called after loading from plugin folder
    private void updateCurrentLocalLanguageFile() {
        if (medievalFactions.isDebugEnabled()) {
            System.out.println("[DEBUG] LocaleManager is updating supported local language files.");
        }
        String ID = configService.getString("languageid");
        if (isLanguageIDSupported(ID)) {
            InputStream inputStream;
            inputStream = getResourceAsInputStream(ID + ".tsv");
            loadMissingKeysFromInputStream(inputStream);
        } else {
            InputStream inputStream;
            inputStream = getResourceAsInputStream("en-us.tsv");
            loadMissingKeysFromInputStream(inputStream);
        }
    }

    private InputStream getResourceAsInputStream(String fileName) {
        return medievalFactions.getResource(fileName);
    }

    private void loadFromResource() {
        try {
            // get resource as input stream
            InputStream inputStream = getResourceAsInputStream(localizationFileName);
            if (inputStream == null) {
                System.out.println("[DEBUG] localization file '" + localizationFileName + "'was not found!");
                return;
            }
            loadMissingKeysFromInputStream(inputStream);
            saveToPluginFolder();
        } catch (Exception e) {
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] Error loading from resource!");
            }
            e.printStackTrace();
        }
    }

    private void loadMissingKeysFromInputStream(InputStream inputStream) {
        InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        BufferedReader br = new BufferedReader(reader);
        br.lines().forEach(line -> {
            Pair<String, String> pair = getPairFromLine(line);
            if (pair != null && !strings.containsKey(pair.getLeft())) { // if pair found and if key not already loaded
                strings.put(pair.getLeft(), pair.getRight());
                keys.add(pair.getLeft());
                System.out.println(String.format("[DEBUG] Loaded missing key %s from resources!", pair.getLeft()));
            }
        });
    }

    private Pair<String, String> getPairFromLine(String line) {
        String key;
        String value = "";

        int tabIndex = getIndexOfTab(line);

        if (tabIndex != -1) {
            key = line.substring(0, tabIndex);
            value = line.substring(tabIndex + 1);
            return new Pair<>(key, value);
        } else {
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
                if (!folder.mkdir()) {
                    if (medievalFactions.isDebugEnabled()) {
                        System.out.println("[DEBUG] Failed to create directory.");
                    }
                    return;
                }
            }
            File file = new File(localizationFilePath);
            if (!file.exists()) {
                if (!file.createNewFile()) {
                    if (medievalFactions.isDebugEnabled()) {
                        System.out.println("[DEBUG] Failed to create file.");
                    }
                    return;
                }
            }
            try (BufferedWriter output = new BufferedWriter(new OutputStreamWriter(
                    new FileOutputStream(file), StandardCharsets.UTF_8))) {
                for (String key : keys) {
                    output.write(key + "\t" + strings.get(key) + "\n");
                }
            } catch (Exception ex) {
                if (medievalFactions.isDebugEnabled()) {
                    System.out.println("[DEBUG] Failed to write to file.");
                }
            }
        } catch (Exception e) {
            if (medievalFactions.isDebugEnabled()) {
                System.out.println("[DEBUG] There was a problem saving the strings.");
            }
            e.printStackTrace();
        }
    }

    private void sortKeys() {
        Collections.sort(keys);
    }

}
