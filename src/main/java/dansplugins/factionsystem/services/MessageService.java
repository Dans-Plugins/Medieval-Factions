package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;

public class MessageService {


    private File languageFile;
    private FileConfiguration language;

    public void createLanguageFile() {
        languageFile = new File(new MedievalFactions().getMedievalFactions().getDataFolder(), "language.yml");
        if (!languageFile.exists()) new MedievalFactions().getMedievalFactions().saveResource("language.yml", false);

        language = new YamlConfiguration();
        try {
            language.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            e.printStackTrace();
        }
    }

    public FileConfiguration getLanguage() {
        return language;
    }

    public void reloadLanguage() {
        language = YamlConfiguration.loadConfiguration(languageFile);
    }

    public void saveLanguage() {
        try {
            language.save(languageFile);
        } catch (IOException ignored) {
        }
    }

}
