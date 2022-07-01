/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.inherited.PlayerRecord;
import dansplugins.factionsystem.services.ConfigService;
import preponderous.ponder.misc.abs.Savable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class PowerRecord extends PlayerRecord implements Savable {
    private final ConfigService configService;
    private final PersistentData persistentData;

    private double powerLevel = 0;

    public PowerRecord(UUID playerUUID, ConfigService configService, PersistentData persistentData, int initial) {
        this.configService = configService;
        this.persistentData = persistentData;
        this.playerUUID = playerUUID;
        powerLevel = initial;
    }

    public PowerRecord(Map<String, String> data, ConfigService configService, PersistentData persistentData) {
        this.configService = configService;
        this.persistentData = persistentData;
        this.load(data);
    }

    public int maxPower() {
        if (isPlayerAFactionOwner(playerUUID)) {
            return (int) (configService.getDouble("initialMaxPowerLevel") * configService.getDouble("factionOwnerMultiplier"));
        }

        if (isPlayerAFactionOfficer(playerUUID)) {
            return (int) (configService.getDouble("initialMaxPowerLevel") * configService.getDouble("factionOfficerMultiplier"));
        }

        return configService.getInt("initialMaxPowerLevel");
    }

    private boolean isPlayerAFactionOwner(UUID player) {
        if (persistentData.isInFaction(player)) {
            Faction faction = persistentData.getPlayersFaction(player);
            return faction.getOwner().equals(player);
        } else {
            return false;
        }
    }

    private boolean isPlayerAFactionOfficer(UUID player) {
        if (persistentData.isInFaction(player)) {
            Faction faction = persistentData.getPlayersFaction(player);
            return faction.isOfficer(player);
        } else {
            return false;
        }
    }

    public void increasePower() {
        if (powerLevel < maxPower()) {
            powerLevel += configService.getInt("powerIncreaseAmount");
            if (powerLevel > maxPower()) {
                powerLevel = maxPower();
            }
        }
    }

    public void decreasePower() {
        if (powerLevel > 0) {
            powerLevel -= configService.getInt("powerDecreaseAmount");
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

    public void grantPowerDueToKill() {
        double powerGained = configService.getDouble("powerGainedOnKill");
        powerLevel = Math.min(powerLevel + powerGained, maxPower());
    }

    public double revokePowerDueToDeath() {
        double powerLost = configService.getDouble("powerLostOnDeath");
        powerLevel = Math.max(powerLevel - powerLost, 0);
        return powerLost;
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
        attemptToLoadPowerLevel(gson, data);

    }

    private void attemptToLoadPowerLevel(Gson gson, Map<String, String> data) {
        try {
            powerLevel = gson.fromJson(data.get("powerLevel"), Double.TYPE);
        } catch (Exception e) {
            powerLevel = gson.fromJson(data.get("powerLevel"), Integer.TYPE);
        }
    }
}