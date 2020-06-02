package plugin;

import org.bukkit.plugin.java.JavaPlugin;

public class Main extends JavaPlugin {

    Faction myFaction = new Faction("Leyton");

    @Override
    public void onEnable() {
        System.out.println("Medieval Factions plugin enabling....");

        myFaction.load("Leyton.txt");

//        myFaction.changeDescription("Led by King Marquess");
//        myFaction.addMember("Dan");
//        myFaction.addMember("Cardinal");

        System.out.println("Medieval Factions plugin enabled.");
    }

    @Override
    public void onDisable(){
        myFaction.save();
    }
}
