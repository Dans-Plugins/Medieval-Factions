package dansplugins.factionsystem;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.*;
import dansplugins.factionsystem.utils.ArgumentParser;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import dansplugins.factionsystem.utils.UUIDChecker;

/*
    This class gives developers access to the internal API for Medieval Factions.
*/
@Deprecated
public class SingletonShelf {

    // instance getters

    public MedievalFactions getPlugin() {
        return MedievalFactions.getInstance();
    }

    public LocalChunkService getChunkManager() {
        return LocalChunkService.getInstance();
    }

    public CommandInterpreter getCommandInterpreter() {
        return CommandInterpreter.getInstance();
    }

    public LocalConfigService getConfigManager() {
        return LocalConfigService.getInstance();
    }

    public DynmapIntegrator getDynmapManager() {
        return DynmapIntegrator.getInstance();
    }

    public LocalLocaleService getLocaleManager() {
        return LocalLocaleService.getInstance();
    }

    public Messenger getMessenger() {
        return Messenger.getInstance();
    }

    public Scheduler getScheduler() {
        return Scheduler.getInstance();
    }

    public LocalStorageService getStorageManager() {
        return LocalStorageService.getInstance();
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

    public LocalLockService getLockManager() {
        return LocalLockService.getInstance();
    }

    public LocalGateService getGateManager() {
        return LocalGateService.getInstance();
    }

    public InteractionAccessChecker getInteractionAccessChecker() {
        return InteractionAccessChecker.getInstance();
    }

}
