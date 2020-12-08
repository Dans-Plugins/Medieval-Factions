package factionsystem;

import factionsystem.EventHandlers.*;

public class EventRegistry {
    public static void registerEvents() {

        // blocks and interaction
        MedievalFactions.getInstance().getServer().getPluginManager().registerEvents(new BlockInteractionHandler(), MedievalFactions.getInstance());

        // joining, leaving and spawning
        MedievalFactions.getInstance().getServer().getPluginManager().registerEvents(new JoiningLeavingAndSpawningHandler(), MedievalFactions.getInstance());

        // damage, effects and death
        MedievalFactions.getInstance().getServer().getPluginManager().registerEvents(new DamageEffectsAndDeathHandler(), MedievalFactions.getInstance());

        // movement
        MedievalFactions.getInstance().getServer().getPluginManager().registerEvents(new MoveHandler(), MedievalFactions.getInstance());

        // chat
        MedievalFactions.getInstance().getServer().getPluginManager().registerEvents(new ChatHandler(), MedievalFactions.getInstance());
        
    }
}
