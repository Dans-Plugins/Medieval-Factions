package dansplugins.factionsystem;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.utils.ArgumentParser;

public class MedievalFactionsAPI {

    public MedievalFactions getMedievalFactions() {
        return MedievalFactions.getInstance();
    }

    public ChunkManager getChunkManager() {
        return ChunkManager.getInstance();
    }

    public CommandInterpreter getCommandInterpreter() {
        return CommandInterpreter.getInstance();
    }

    public ConfigManager getConfigManager() {
        return ConfigManager.getInstance();
    }

    public DynmapManager getDynmapManager() {
        return DynmapManager.getInstance();
    }

    public LocaleManager getLocaleManager() {
        return LocaleManager.getInstance();
    }

    public Messenger getMessenger() {
        return Messenger.getInstance();
    }

    public Scheduler getScheduler() {
        return Scheduler.getInstance();
    }

    public StorageManager getStorageManager() {
        return StorageManager.getInstance();
    }

    public UUIDChecker getUUIDChecker() {
        return UUIDChecker.getInstance();
    }

    public PersistentData getPersistentData() {
        return PersistentData.getInstance();
    }

    public EphemeralData getEphemeralData() {
        return EphemeralData.getInstance();
    }

    public ArgumentParser getArgumentParser() {
        return ArgumentParser.getInstance();
    }

}
