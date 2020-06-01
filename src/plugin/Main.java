package plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    Faction myFaction = new Faction("Alexandria");

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        myFaction.load("Leyton");

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        myFaction.save();
    }
}