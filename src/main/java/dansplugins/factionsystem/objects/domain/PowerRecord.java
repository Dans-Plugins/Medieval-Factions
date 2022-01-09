package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.inherited.PlayerRecord;
import preponderous.ponder.misc.Savable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * @author Daniel McCoy Stephenson
 */
public class PowerRecord extends PlayerRecord implements Savable {
    private int powerLevel = 0;

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
        if (isPlayerAFactionOwner(playerUUID, PersistentData.getInstance().getFactions())){
            return (int) (MedievalFactions.getInstance().getConfig().getDouble("initialMaxPowerLevel") * MedievalFactions.getInstance().getConfig().getDouble("factionOwnerMultiplier", 2.0));
        }

        if (isPlayerAFactionOfficer(playerUUID, PersistentData.getInstance().getFactions())){
            return (int) (MedievalFactions.getInstance().getConfig().getDouble("initialMaxPowerLevel") * MedievalFactions.getInstance().getConfig().getDouble("factionOfficerMultiplier", 1.5));
        }

        return MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel");
    }

    private boolean isPlayerAFactionOwner(UUID player, ArrayList<Faction> factions){
        if (PersistentData.getInstance().isInFaction(player)){
            Faction faction = PersistentData.getInstance().getPlayersFaction(player);
            return faction.getOwner().equals(player);
        } else {
            return false;
        }
    }

    private boolean isPlayerAFactionOfficer(UUID player, ArrayList<Faction> factions) {
        if (PersistentData.getInstance().isInFaction(player)){
            Faction faction = PersistentData.getInstance().getPlayersFaction(player);
            return faction.isOfficer(player);
        } else {
            return false;
        }
    }

    public boolean increasePower() {
        if (powerLevel < maxPower()) {
        	powerLevel += MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount");
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
            powerLevel -= MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount");
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