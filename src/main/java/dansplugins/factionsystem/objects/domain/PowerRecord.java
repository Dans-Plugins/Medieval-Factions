/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.inherited.PlayerRecord;
import preponderous.ponder.misc.Savable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class PowerRecord extends PlayerRecord implements Savable {
    private double powerLevel = 0;

    public PowerRecord(UUID playerUUID, int initial) {
        this.playerUUID = playerUUID;
        powerLevel = initial;
    }

    public PowerRecord(Map<String, String> data) {
        this.load(data);
    }

    public PowerRecord() {

    }

    public int maxPower() {
        if (isPlayerAFactionOwner(playerUUID)){
            return (int) (MedievalFactions.getInstance().getConfig().getDouble("initialMaxPowerLevel") * MedievalFactions.getInstance().getConfig().getDouble("factionOwnerMultiplier", 2.0));
        }

        if (isPlayerAFactionOfficer(playerUUID)){
            return (int) (MedievalFactions.getInstance().getConfig().getDouble("initialMaxPowerLevel") * MedievalFactions.getInstance().getConfig().getDouble("factionOfficerMultiplier", 1.5));
        }

        return MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel");
    }

    private boolean isPlayerAFactionOwner(UUID player){
        if (PersistentData.getInstance().isInFaction(player)){
            Faction faction = PersistentData.getInstance().getPlayersFaction(player);
            return faction.getOwner().equals(player);
        } else {
            return false;
        }
    }

    private boolean isPlayerAFactionOfficer(UUID player) {
        if (PersistentData.getInstance().isInFaction(player)){
            Faction faction = PersistentData.getInstance().getPlayersFaction(player);
            return faction.isOfficer(player);
        } else {
            return false;
        }
    }

    public void increasePower() {
        if (powerLevel < maxPower()) {
        	powerLevel += MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount");
        	if (powerLevel > maxPower()) {
                powerLevel = maxPower();
            }
        }
    }

    public void decreasePower() {
        if (powerLevel > 0) {
            powerLevel -= MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount");
            if (powerLevel < 0) {
                powerLevel = 0;
            }
        }
    }

    public double getPower() {
        return powerLevel;
    }

    public void setPower(double newPower) {
        powerLevel = newPower;
    }

    public boolean increasePowerByTenPercent() {
        double originalLevel = powerLevel;
        double newLevel = powerLevel * 1.10;

        if (originalLevel == newLevel){
            newLevel++;
        }

        powerLevel = Math.min(newLevel, maxPower());
        if (powerLevel == 0){
            powerLevel = 1;
        }
        return powerLevel != originalLevel;
    }

    public int decreasePowerByTenPercent() {
        int powerDecreaseAmount = (int) (powerLevel * 0.10);
        powerLevel =- powerDecreaseAmount;
        if (powerLevel < 0) {
            powerLevel = 0;
        }
        return powerDecreaseAmount;
    }

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("playerUUID", gson.toJson(playerUUID.toString()));
        saveMap.put("powerLevel", gson.toJson(powerLevel));

        return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        playerUUID = UUID.fromString(gson.fromJson(data.get("playerUUID"), String.class));
        powerLevel = gson.fromJson(data.get("powerLevel"), Integer.TYPE);
    }
}