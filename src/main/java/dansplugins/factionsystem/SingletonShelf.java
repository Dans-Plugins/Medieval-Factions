package dansplugins.factionsystem;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.*;
import dansplugins.factionsystem.utils.ArgumentParser;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import dansplugins.factionsystem.utils.UUIDChecker;

import java.util.UUID;

/*
    This class gives developers access to the internal API for Medieval Factions.
*/
@Deprecated
public class SingletonShelf {

    // instance getters

    public MedievalFactions getPlugin() {
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

    public DynmapIntegrator getDynmapManager() {
        return DynmapIntegrator.getInstance();
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

    public BlockChecker getBlockChecker() {
        return BlockChecker.getInstance();
    }

    public LockManager getLockManager() {
        return LockManager.getInstance();
    }

    public GateManager getGateManager() {
        return GateManager.getInstance();
    }

    public InteractionAccessChecker getInteractionAccessChecker() {
        return InteractionAccessChecker.getInstance();
    }

}
