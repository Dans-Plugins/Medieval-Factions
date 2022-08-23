package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageService {


    private static File languageFile;
    private static FileConfiguration language;

    public static void createLanguageFile() {
        languageFile = new File(MedievalFactions.getMedievalFactions().getDataFolder(), "language.yml");
        if (!languageFile.exists()) MedievalFactions.getMedievalFactions().saveResource("language.yml", false);

        language = new YamlConfiguration();
        try {
            language.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public static FileConfiguration getLanguage() {
        return language;
    }

    public static void reloadLanguage() {
        language = YamlConfiguration.loadConfiguration(languageFile);
    }

    public static void saveLanguage() {
        try {
            language.save(languageFile);
        } catch (IOException ignored) {
        }
    }

}
