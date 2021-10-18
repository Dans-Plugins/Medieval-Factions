package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.objects.domain.specification.IFaction;
import dansplugins.factionsystem.objects.helper.FactionFlags;
import dansplugins.factionsystem.objects.inherited.Nation;
import dansplugins.factionsystem.objects.inherited.specification.modifiers.Feudal;
import dansplugins.factionsystem.objects.inherited.specification.modifiers.Savable;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class Faction extends Nation implements IFaction, Feudal, Savable {

    // persistent data
    private ArrayList<String> vassals = new ArrayList<>();
    private ArrayList<Gate> gates = new ArrayList<>();
    private String liege = "none";
    private String prefix = "none";
    private Location factionHome = null;
    private FactionFlags flags = new FactionFlags();
    private int bonusPower = 0;

    // ephemeral data
    private ArrayList<String> attemptedVassalizations = new ArrayList<>();
    private boolean autoclaim = false;

    // player constructor
    public Faction(String initialName, UUID creator) {
        setName(initialName);
        setOwner(creator);
        prefix = initialName;
        flags.initializeFlagValues();
    }

    // server constructor
    public Faction(String initialName) {
        setName(initialName);
        prefix = initialName;
        flags.initializeFlagValues(); // need to ensure that this doesn't mess up changes to flags being persistent
    }

    @Override
    public ArrayList<Gate> getGates() {
    	return gates;
    }

    // Must receive json data
    public Faction(Map<String, String> data) {
        this.load(data);
    }

    // implementations for IFaction methods ------------------------------

    @Override
    public void setFactionHome(Location l) {
        factionHome = l;
    }

    @Override
    public int getTotalGates() {
        return gates.size();
    }

    @Override
    public String getPrefix() {
        return prefix;
    }

    @Override
    public void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }

    @Override
    public Location getFactionHome() {
        return factionHome;
    }

    @Override
    public FactionFlags getFlags() {
        return flags;
    }

    @Override
    public int getBonusPower() {
        return bonusPower;
    }

    @Override
    public void setBonusPower(int i) {
        bonusPower = i;
    }

    @Override
    public void toggleAutoClaim() {
        autoclaim = !autoclaim;
    }

    @Override
    public boolean getAutoClaimStatus() {
        return autoclaim;
    }

    @Override
    public String getTopLiege() {
        Faction topLiege = PersistentData.getInstance().getFaction(liege);
        String liegeName = liege;
        while (topLiege != null) {
            topLiege = PersistentData.getInstance().getFaction(topLiege.getLiege());
            if (topLiege != null)
            {
                liegeName = topLiege.getName();
            }
        }
        return liegeName;
    }

    @Override
    public int calculateCumulativePowerLevelWithoutVassalContribution() {
        int powerLevel = 0;
        for (UUID playerUUID : members) {
            try {
                powerLevel += PersistentData.getInstance().getPlayersPowerRecord(playerUUID).getPowerLevel();
            }
            catch (Exception e) {
                System.out.println(LocaleManager.getInstance().getText("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return powerLevel;
    }

    @Override
    public int calculateCumulativePowerLevelWithVassalContribution() {
        int vassalContribution = 0;
        double percentage = MedievalFactions.getInstance().getConfig().getDouble("vassalContributionPercentageMultiplier");
        for (String factionName : vassals) {
            Faction vassalFaction = PersistentData.getInstance().getFaction(factionName);
            if (vassalFaction != null) {
                vassalContribution += vassalFaction.getCumulativePowerLevel() * percentage;
            }
        }
        return calculateCumulativePowerLevelWithoutVassalContribution() + vassalContribution;
    }

    @Override
    public int getCumulativePowerLevel() {
        int withoutVassalContribution = calculateCumulativePowerLevelWithoutVassalContribution();
        int withVassalContribution = calculateCumulativePowerLevelWithVassalContribution();

        if (vassals.size() == 0 || (withoutVassalContribution < (getMaximumCumulativePowerLevel() / 2))) {
            return withoutVassalContribution + bonusPower;
        }
        else {
            return withVassalContribution + bonusPower;
        }
    }

    @Override
    public int getMaximumCumulativePowerLevel() {     // get max power without vassal contribution
        int maxPower = 0;

        for (UUID playerUUID : members){
            try
            {
                maxPower += PersistentData.getInstance().getPlayersPowerRecord(playerUUID).maxPower();
            }
            catch (Exception e)
            {
                System.out.println(LocaleManager.getInstance().getText("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }
        return maxPower;
    }

    @Override
    public int calculateMaxOfficers(){
        int officersPerXNumber = MedievalFactions.getInstance().getConfig().getInt("officerPerMemberCount");
        int officersFromConfig = members.size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    @Override
    public List<ClaimedChunk> getClaimedChunks() {
        List<ClaimedChunk> output = new ArrayList<>();
        for (ClaimedChunk chunk : PersistentData.getInstance().getClaimedChunks()) {
            if (chunk.getHolder().equalsIgnoreCase(getName())) {
                output.add(chunk);
            }
        }
        return output;
    }

    @Override
    public boolean isWeakened() {
        return calculateCumulativePowerLevelWithoutVassalContribution() < (getMaximumCumulativePowerLevel() / 2);
    }

    /**
     * Method to automatically handle all data changes when a Faction changes their name.
     * @param oldName of the Faction (dependent).
     * @param newName of the Faction (dependent).
     */
    @Override
    public void updateData(String oldName, String newName) {
        if (isAlly(oldName)) {
            removeAlly(oldName);
            addAlly(newName);
        }
        if (isEnemy(oldName)) {
            removeEnemy(oldName);
            addEnemy(newName);
        }
        if (isLiege(oldName)) {
            setLiege(newName);
        }
        if (isVassal(oldName)) {
            removeVassal(oldName);
            addVassal(newName);
        }
    }

    // end of implementations for IFaction methods ------------------------------



    // implementations for Feudal methods ------------------------------

    @Override
    public boolean isVassal(String faction) {
        return(containsIgnoreCase(vassals, faction));
    }

    @Override
    public boolean isLiege(String faction) {
        return liege.equalsIgnoreCase(faction);
    }

    @Override
    public void addVassal(String name) {
        if (!containsIgnoreCase(vassals, name)) {
            vassals.add(name);
        }
    }

    @Override
    public void removeVassal(String name) {
        removeIfContainsIgnoreCase(vassals, name);
    }


    // unsorted -----------------------






    @Override
    public boolean addOfficer(UUID newOfficer) {
        if (officers.size() < calculateMaxOfficers() && !officers.contains(newOfficer)){
            officers.add(newOfficer);
            return true;
        } else {
            return false;
        }
    }

    @Override
    public String toString() {
        return "Faction{" +
                "members=" + members +
                ", enemyFactions=" + enemyFactions +
                ", officers=" + officers +
                ", allyFactions=" + allyFactions +
                ", laws=" + laws +
                ", name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", owner=" + owner +
                ", cumulativePowerLevel=" + getCumulativePowerLevel() +
                ", liege=" + liege +
                '}';
    }



    @Override
    public String getVassalsSeparatedByCommas() {
        String toReturn = "";
        for (int i = 0; i < vassals.size(); i++) {
            toReturn = toReturn + vassals.get(i);
            if (i != vassals.size() - 1) {
                toReturn = toReturn + ", ";
            }
        }
        return toReturn;
    }

    @Override
    public void addAttemptedVassalization(String factionName) {
        if (!containsIgnoreCase(attemptedVassalizations, factionName)) {
            attemptedVassalizations.add(factionName);
        }
    }

    @Override
    public boolean hasBeenOfferedVassalization(String factionName) {
        return containsIgnoreCase(attemptedVassalizations, factionName);
    }

    @Override
    public void removeAttemptedVassalization(String factionName) {
        removeIfContainsIgnoreCase(attemptedVassalizations, factionName);
    }

    private boolean containsIgnoreCase(ArrayList<String> list, String str) {
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                return true;
            }
        }
        return false;
    }

    private void removeIfContainsIgnoreCase(ArrayList<String> list, String str) {
        String toRemove = "";
        for (String string : list) {
            if (string.equalsIgnoreCase(str)) {
                toRemove = string;
                break;
            }
        }
        list.remove(toRemove);
    }

    @Override
    public void clearVassals() {
        vassals.clear();
    }

    @Override
    public int getNumVassals() {
        return vassals.size();
    }

    @Override
    public ArrayList<String> getVassals() {
        return vassals;
    }

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;
        Map<String, String> saveMap = new HashMap<>();

        saveMap.put("members", gson.toJson(members));
        saveMap.put("enemyFactions", gson.toJson(enemyFactions));
        saveMap.put("officers", gson.toJson(officers));
        saveMap.put("allyFactions", gson.toJson(allyFactions));
        saveMap.put("laws", gson.toJson(laws));
        saveMap.put("name", gson.toJson(name));
        saveMap.put("vassals", gson.toJson(vassals));
        saveMap.put("description", gson.toJson(description));
        saveMap.put("owner", gson.toJson(owner));
        saveMap.put("location", gson.toJson(saveLocation(gson)));
        saveMap.put("liege", gson.toJson(liege));
        saveMap.put("prefix", gson.toJson(prefix));
        saveMap.put("bonusPower", gson.toJson(bonusPower));

        ArrayList<String> gateList = new ArrayList<String>();
        for (Gate gate : gates)
        {
            Map <String, String> map = gate.save();
            gateList.add(gson.toJson(map));
        }
        saveMap.put("factionGates", gson.toJson(gateList));

        saveMap.put("integerFlagValues", gson.toJson(flags.getIntegerValues()));
        saveMap.put("booleanFlagValues", gson.toJson(flags.getBooleanValues()));
        saveMap.put("doubleFlagValues", gson.toJson(flags.getDoubleValues()));
        saveMap.put("stringFlagValues", gson.toJson(flags.getStringValues()));

        return saveMap;
    }

    private Map<String, String> saveLocation(Gson gson) {
        Map<String, String> saveMap = new HashMap<>();

        if (factionHome != null && factionHome.getWorld() != null){
            saveMap.put("worldName", gson.toJson(factionHome.getWorld().getName()));
            saveMap.put("x", gson.toJson(factionHome.getX()));
            saveMap.put("y", gson.toJson(factionHome.getY()));
            saveMap.put("z", gson.toJson(factionHome.getZ()));
        }

        return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        Type arrayListTypeString = new TypeToken<ArrayList<String>>(){}.getType();
        Type arrayListTypeUUID = new TypeToken<ArrayList<UUID>>(){}.getType();
        Type stringToIntegerMapType = new TypeToken<HashMap<String, Integer>>(){}.getType();
        Type stringToBooleanMapType = new TypeToken<HashMap<String, Boolean>>(){}.getType();
        Type stringToDoubleMapType = new TypeToken<HashMap<String, Double>>(){}.getType();
        Type stringToStringMapType = new TypeToken<HashMap<String, String>>(){}.getType();

        members = gson.fromJson(data.get("members"), arrayListTypeUUID);
        enemyFactions = gson.fromJson(data.get("enemyFactions"), arrayListTypeString);
        officers = gson.fromJson(data.get("officers"), arrayListTypeUUID);
        allyFactions = gson.fromJson(data.get("allyFactions"), arrayListTypeString);
        laws = gson.fromJson(data.get("laws"), arrayListTypeString);
        name = gson.fromJson(data.get("name"), String.class);
        description = gson.fromJson(data.get("description"), String.class);
        owner = UUID.fromString(gson.fromJson(data.get("owner"), String.class));
        factionHome = loadLocation(gson.fromJson(data.get("location"), stringToStringMapType), gson);
        liege = gson.fromJson(data.getOrDefault("liege", "none"), String.class);
        vassals = gson.fromJson(data.getOrDefault("vassals", "[]"), arrayListTypeString);
        prefix = loadDataOrDefault(gson, data, "prefix", getName());
        bonusPower = gson.fromJson(data.getOrDefault("bonusPower", "0"), Integer.TYPE);

//        System.out.println("Loading Faction Gates...");
        ArrayList<String> gateList = new ArrayList<String>();
        gateList = gson.fromJson(data.get("factionGates"), arrayListTypeString);
        if (gateList != null)
        {
            for (String item : gateList)
            {
                Gate g = Gate.load(item);
                gates.add(g);
            }
        }
        else
        {
            System.out.println(LocaleManager.getInstance().getText("MissingFactionGatesJSONCollection"));
        }

        flags.setIntegerValues(gson.fromJson(data.getOrDefault("integerFlagValues", "[]"), stringToIntegerMapType));
        flags.setBooleanValues(gson.fromJson(data.getOrDefault("booleanFlagValues", "[]"), stringToBooleanMapType));
        flags.setDoubleValues(gson.fromJson(data.getOrDefault("doubleFlagValues", "[]"), stringToDoubleMapType));
        flags.setStringValues(gson.fromJson(data.getOrDefault("stringFlagValues", "[]"), stringToStringMapType));

        flags.loadMissingFlagsIfNecessary();
    }

    private String loadDataOrDefault(Gson gson, Map<String, String> data, String key, String def) {
        try {
            return gson.fromJson(data.getOrDefault(key, def), String.class);
        } catch(Exception e) {
            return def;
        }
    }

    private Location loadLocation(HashMap<String, String> data, Gson gson){
        if (data.size() != 0){
            World world = getServer().createWorld(new WorldCreator(gson.fromJson(data.get("worldName"), String.class)));
            double x = gson.fromJson(data.get("x"), Double.TYPE);
            double y = gson.fromJson(data.get("y"), Double.TYPE);
            double z = gson.fromJson(data.get("z"), Double.TYPE);
            return new Location(world, x, y, z);
        }
        return null;
    }

}