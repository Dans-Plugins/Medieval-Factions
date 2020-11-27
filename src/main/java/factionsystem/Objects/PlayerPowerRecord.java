package factionsystem.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import factionsystem.Main;
import factionsystem.Subsystems.UtilitySubsystem;
import org.bukkit.OfflinePlayer;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;
import static org.bukkit.Bukkit.getServer;

public class PlayerPowerRecord {

    // saved
    private UUID playerUUID = UUID.randomUUID();
    private int powerLevel = 0;

    // temporary
    private Main main;

    public PlayerPowerRecord(UUID playerUUID, int initial, Main main) {
        this.playerUUID = playerUUID;
        powerLevel = initial;
        this.main = main;
    }
    public PlayerPowerRecord(Main main) { // server constructor for loading
        this.main = main;
    }

    public PlayerPowerRecord(Map<String, String> data, Main main) {
        this.load(data);
        this.main = main;
    }

    public int maxPower() {
        if (UtilitySubsystem.isPlayerAFactionOwner(playerUUID, main.factions)){
            return (int) (main.getConfig().getDouble("initialMaxPowerLevel") * main.getConfig().getDouble("factionOwnerMultiplier", 2.0));
        }

        if (UtilitySubsystem.isPlayerAFactionOfficer(playerUUID, main.factions)){
            return (int) (main.getConfig().getDouble("initialMaxPowerLevel") * main.getConfig().getDouble("factionOfficerMultiplier", 1.5));
        }

        return main.getConfig().getInt("initialMaxPowerLevel");
    }


    public void setPlayerName(UUID UUID) {
        playerUUID = UUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean increasePower() {
        if (powerLevel < maxPower()) {
        	powerLevel += main.getConfig().getInt("powerIncreaseAmount");
        	if (powerLevel > maxPower())
        		powerLevel = maxPower();
        	
            return true;
        }
        else {
            return false;
        }
    }

    public boolean decreasePower() {
        if (powerLevel > 0) {
            powerLevel -= main.getConfig().getInt("powerDecreaseAmount");
            if (powerLevel < 0)
            	powerLevel = 0;
            return true;
        }
        else {
            return false;
        }
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void setPowerLevel(int newPower) {
        powerLevel = newPower;
    }

    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("playerUUID", gson.toJson(playerUUID.toString()));
        saveMap.put("powerLevel", gson.toJson(powerLevel));

        return saveMap;
    }

    private void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        playerUUID = UUID.fromString(gson.fromJson(data.get("playerUUID"), String.class));
        powerLevel = gson.fromJson(data.get("powerLevel"), Integer.TYPE);
    }

    public void legacyLoad(String filename) {
        try {
            File loadFile = new File("./plugins/MedievalFactions/player-power-records/" + filename);
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            if (loadReader.hasNextLine()) {
                playerUUID = findUUIDBasedOnPlayerName(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                powerLevel = Integer.parseInt(loadReader.nextLine());
            }

            loadReader.close();

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
        }
    }

    /**
     * @return True if powerlevel changed else false
     */
    public boolean increasePowerByTenPercent() {
//        System.out.println("Original Power:" + powerLevel);
        int originalLevel = powerLevel;
        int newLevel = (int) (powerLevel * 1.10);

        // If not 10 percent, then add 1!
        if (originalLevel == newLevel){
            newLevel++;
        }

        powerLevel = Math.min(newLevel, maxPower());
//        System.out.println("End power level:" + powerLevel);
        if (powerLevel == 0){
            powerLevel = 1;
        }
        return powerLevel != originalLevel;
    }

    public void decreasePowerByTenPercent() {
        int newLevel = (int) (powerLevel * 0.90);
        if (powerLevel > 0){
            powerLevel = newLevel;
        } else {
            powerLevel = 0;
        }
    }
}
