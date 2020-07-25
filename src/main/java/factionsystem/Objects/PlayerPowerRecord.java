package factionsystem.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import factionsystem.Main;
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
    int maxPower = 0;

    public PlayerPowerRecord(UUID playerUUID, int initial, int max) {
        this.playerUUID = playerUUID;
        powerLevel = initial;
        maxPower = max;
    }
    public PlayerPowerRecord(int max) { // server constructor for loading
        maxPower = max;
    }

    public PlayerPowerRecord(Map<String, String> data, Main main) {
        this.load(data);
        maxPower = main.getConfig().getInt("maxPowerLevel");
    }

    public void setPlayerName(UUID UUID) {
        playerUUID = UUID;
    }

    public UUID getPlayerUUID() {
        return playerUUID;
    }

    public boolean increasePower() {
        if (powerLevel < maxPower) {
            powerLevel++;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean decreasePower() {
        if (powerLevel > 0) {
            powerLevel--;
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
        System.out.println("Original Power:" + powerLevel);
        int originalLevel = powerLevel;
        int newLevel = (int) (powerLevel * 1.10);

        // If not 10 percent, then add 1!
        if (originalLevel == newLevel){
            newLevel++;
        }

        powerLevel = Math.min(newLevel, maxPower);
        System.out.println("End power level:" + powerLevel);
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
