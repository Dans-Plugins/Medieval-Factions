package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;

public class MessageService {

    private final MedievalFactions medievalFactions;
    private File languageFile;
    private FileConfiguration language;

    public MessageService(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
    }

    public void createLanguageFile() {
        languageFile = new File(medievalFactions.getDataFolder(), "language.yml");
        if (!languageFile.exists()) medievalFactions.saveResource("language.yml", false);

        language = new YamlConfiguration();
        try {
            language.load(languageFile);
        } catch (IOException | InvalidConfigurationException e) {
            medievalFactions.getLogger().log(Level.WARNING, e.getCause().toString());
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
