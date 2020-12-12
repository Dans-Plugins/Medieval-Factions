package dansplugins.factionsystem;

import dansplugins.factionsystem.eventhandlers.*;
import org.bukkit.plugin.PluginManager;

public class EventRegistry {

    private static EventRegistry instance;

    private EventRegistry() {

    }

    public static EventRegistry getInstance() {
        if (instance == null) {
            instance = new EventRegistry();
        }
        return instance;
    }

    public void registerEvents() {

        MedievalFactions mainInstance = MedievalFactions.getInstance();
        PluginManager manager = mainInstance.getServer().getPluginManager();

        // blocks and interaction
        manager.registerEvents(new BlockInteractionHandler(), mainInstance);

        // joining, leaving and spawning
        manager.registerEvents(new JoiningLeavingAndSpawningHandler(), mainInstance);

        // damage, effects and death
        manager.registerEvents(new DamageEffectsAndDeathHandler(), mainInstance);

        // movement
        manager.registerEvents(new MoveHandler(), mainInstance);

        // chat
        manager.registerEvents(new ChatHandler(), mainInstance);
        
    }
}
