package dansplugins.factionsystem;

import dansplugins.factionsystem.eventhandlers.*;
import factionsystem.EventHandlers.*;
import org.bukkit.plugin.PluginManager;

public class EventRegistry {
    public static void registerEvents() {

        MedievalFactions instance = MedievalFactions.getInstance();
        PluginManager manager = instance.getServer().getPluginManager();

        // blocks and interaction
        manager.registerEvents(new BlockInteractionHandler(), instance);

        // joining, leaving and spawning
        manager.registerEvents(new JoiningLeavingAndSpawningHandler(), instance);

        // damage, effects and death
        manager.registerEvents(new DamageEffectsAndDeathHandler(), instance);

        // movement
        manager.registerEvents(new MoveHandler(), instance);

        // chat
        manager.registerEvents(new ChatHandler(), instance);
        
    }
}
