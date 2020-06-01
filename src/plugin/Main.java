package plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    Faction myFaction = new Faction("Leyton");

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        myFaction.load("Leyton.txt");

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        myFaction.save();
    }
}
