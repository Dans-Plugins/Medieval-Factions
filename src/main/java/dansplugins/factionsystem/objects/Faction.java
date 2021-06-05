package dansplugins.factionsystem.objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.lang.reflect.Type;
import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class Faction {

    // persistent data -------------------------------------------------------

    // lists
    private ArrayList<UUID> members = new ArrayList<>();
    private ArrayList<UUID> officers = new ArrayList<>();
    private ArrayList<String> enemyFactions = new ArrayList<>();
    private ArrayList<String> allyFactions = new ArrayList<>();
    private ArrayList<String> laws = new ArrayList<>();
    private ArrayList<String> vassals = new ArrayList<>();
    private ArrayList<Gate> gates = new ArrayList<>();

    // strings
    private String name = "defaultName";
    private String description = "defaultDescription";
    private String liege = "none";
    private String prefix = "none";

    // other
    private UUID owner = UUID.randomUUID();
    private Location factionHome = null;
    private FactionFlags flags = new FactionFlags();
    private int bonusPower = 0;

    // end of persistent data -------------------------------------------------------

    // ephemeral data -------------------------------------------------------

    // lists
    private ArrayList<UUID> invited = new ArrayList<>();
    private ArrayList<String> attemptedTruces = new ArrayList<>();
    private ArrayList<String> attemptedAlliances = new ArrayList<>();
    private ArrayList<String> attemptedVassalizations = new ArrayList<>();

    // other
    private boolean autoclaim = false;

    // end of ephemeral data -------------------------------------------------------

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

    public ArrayList<Gate> getGates() {
    	return gates;
    }    
    
    // Must recieve json data
    public Faction(Map<String, String> data) {
        this.load(data);
    }

    public int getNumOfficers() {
        return officers.size();
    }

    public void addLaw(String newLaw) {
        laws.add(newLaw);
    }

    public boolean removeLaw(String lawToRemove) {
        if (containsIgnoreCase(laws, lawToRemove)) {
            laws.remove(lawToRemove);
            return true;
        }
        return false;
    }

    public boolean removeLaw(int i) {
        if (laws.size() > i) {
            laws.remove(i);
            return true;
        }
        return false;
    }

    public boolean editLaw(int i, String newString) {
        if (laws.size() > i) {
            laws.set(i, newString);
            return true;
        }
        return false;
    }

    public int getNumLaws() {
        return laws.size();
    }

    public ArrayList<String> getLaws() {
        return laws;
    }

    public void requestTruce(String factionName) {
        if (!containsIgnoreCase(attemptedTruces, factionName)) {
            attemptedTruces.add(factionName);
        }
    }

    public boolean isTruceRequested(String factionName) {
        return containsIgnoreCase(attemptedTruces, factionName);
    }

    public void removeRequestedTruce(String factionName) {
        removeIfContainsIgnoreCase(attemptedTruces, factionName);
    }

    public void requestAlly(String factionName) {
        if (!containsIgnoreCase(attemptedAlliances, factionName)) {
            attemptedAlliances.add(factionName);
        }
    }

    public boolean isRequestedAlly(String factionName) {
        return containsIgnoreCase(attemptedAlliances, factionName);
    }

    public void addAlly(String factionName) {
        if (!containsIgnoreCase(allyFactions, factionName)) {
            allyFactions.add(factionName);
        }
    }

    public void removeAlly(String factionName) {
        removeIfContainsIgnoreCase(allyFactions, factionName);
    }

    public boolean isAlly(String factionName) {
        return containsIgnoreCase(allyFactions, factionName);
    }

    public ArrayList<String> getAllies() {
        return allyFactions;
    }

    public void setFactionHome(Location l) {
        factionHome = l;
    }

    public Location getFactionHome() {
        return factionHome;
    }

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

    public int calculateCumulativePowerLevelWithoutVassalContribution() {

        int powerLevel = 0;

        for (UUID playerUUID : members){
            try
            {
                powerLevel += PersistentData.getInstance().getPlayersPowerRecord(playerUUID).getPowerLevel();
            }
            catch (Exception e)
            {
                System.out.println(LocaleManager.getInstance().getText("ErrorPlayerPowerRecordForUUIDNotFound"));
            }
        }

        return powerLevel;
    }

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

    // get max power without vassal contribution
    public int getMaximumCumulativePowerLevel() {
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

    public int calculateMaxOfficers(){
        int officersPerXNumber = MedievalFactions.getInstance().getConfig().getInt("officerPerMemberCount");
        int officersFromConfig = members.size() / officersPerXNumber;
        return 1 + officersFromConfig;
    }

    public boolean addOfficer(UUID newOfficer) {
        if (officers.size() < calculateMaxOfficers() && !officers.contains(newOfficer)){
            officers.add(newOfficer);
            return true;
        } else {
            return false;
        }
    }

    public boolean removeOfficer(UUID officerToRemove) {
        return officers.remove(officerToRemove);
    }

    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    public ArrayList<UUID> getMemberArrayList() {
        return members;
    }

    public void toggleAutoClaim() {
        autoclaim = !autoclaim;
    }

    public boolean getAutoClaimStatus() {
        return autoclaim;
    }

    public void addEnemy(String factionName) {
        if (!containsIgnoreCase(enemyFactions, factionName)) {
            enemyFactions.add(factionName);
        }
    }

    public void removeEnemy(String factionName) {
        removeIfContainsIgnoreCase(enemyFactions, factionName);
    }

    public boolean isEnemy(String factionName) {
        return containsIgnoreCase(enemyFactions, factionName);
    }

    public String getEnemiesSeparatedByCommas() {
        String enemies = "";
        for (int i = 0; i < enemyFactions.size(); i++) {
            enemies = enemies + enemyFactions.get(i);
            if (i != enemyFactions.size() - 1) {
                enemies = enemies + ", ";
            }
        }
        return enemies;
    }

    public String getAlliesSeparatedByCommas() {
        String allies = "";
        for (int i = 0; i < allyFactions.size(); i++) {
            allies = allies + allyFactions.get(i);
            if (i != allyFactions.size() - 1) {
                allies = allies + ", ";
            }
        }
        return allies;
    }

    public List<ClaimedChunk> getClaimedChunks() {
        List<ClaimedChunk> output = new ArrayList<>();
        for (ClaimedChunk chunk : PersistentData.getInstance().getClaimedChunks()) {
            if (chunk.getHolder().equalsIgnoreCase(getName())) {
                output.add(chunk);
            }
        }
        return output;
    }

    public void invite(UUID playerName) {
        Player player = getServer().getPlayer(playerName);
        if (player != null) {
            UUID playerUUID = getServer().getPlayer(playerName).getUniqueId();
            invited.add(playerUUID);
        }
    }

    public void uninvite(UUID player) {
        invited.remove(player);
    }

    public boolean isInvited(UUID uuid) {
        return invited.contains(uuid);
    }

    public ArrayList<UUID> getMemberList() {
        return members;
    }

    public String getMemberListSeparatedByCommas() {
        ArrayList<UUID> uuids = getMemberList();
        String players = "";
        for(UUID uuid : uuids) {
            String playerName = UUIDChecker.getInstance().findPlayerNameBasedOnUUID(uuid);
            players += playerName + ", ";
        }
        if (players.length() > 0) {
            return players.substring(0, players.length() - 2);
        }
        return "";
    }

    public int getPopulation() {
        return members.size();
    }

    public void setOwner(UUID UUID) {
        owner = UUID;
    }

    public boolean isOwner(UUID UUID) {
        return owner.equals(UUID);
    }

    public UUID getOwner() {
        return owner;
    }

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String newDesc) {
        description = newDesc;
    }

    public String getDescription() {
        return description;
    }

    public void addMember(UUID UUID) {
        members.add(UUID);
    }

    public void removeMember(UUID UUID) {
        members.remove(UUID);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

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

    private void load(Map<String, String> data) {
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

    public boolean isVassal(String faction) {
        return(containsIgnoreCase(vassals, faction));
    }

    public boolean hasLiege() {
        return !liege.equalsIgnoreCase("none");
    }

    public boolean isLiege(String faction) {
        return liege.equalsIgnoreCase(faction);
    }

    public void addVassal(String factionName) {
        if (!containsIgnoreCase(vassals, factionName)) {
            vassals.add(factionName);
        }
    }

    public void removeVassal(String faction) {
        removeIfContainsIgnoreCase(vassals, faction);
    }

    public void setLiege(String newLiege) {
        liege = newLiege;
    }

    public void addGate(Gate gate)
    {
    	gates.add(gate);
    }
    
    public void removeGate(Gate gate)
    {
    	gate.fillGate();
    	gates.remove(gate);
    }

    public boolean hasGateTrigger(Block block)
    {
    	for(Gate g : gates)
    	{
    		if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
    				g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName()))
    		{
    			return true;
    		}
    	}
		return false;
    }
    
    public ArrayList<Gate> getGatesForTrigger(Block block)
    {
    	ArrayList<Gate> gateList = new ArrayList<>();
    	for(Gate g : gates)
    	{
    		if (g.getTrigger().getX() == block.getX() && g.getTrigger().getY() == block.getY() && g.getTrigger().getZ() == block.getZ() &&
    				g.getTrigger().getWorld().equalsIgnoreCase(block.getWorld().getName()))
    		{
    			gateList.add(g);
    		}
    	}
		return gateList;
    }

    public String getLiege() {
        return liege;
    }

    public boolean isLiege() {
        return vassals.size() != 0;
    }

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

    public void addAttemptedVassalization(String factionName) {
        if (!containsIgnoreCase(attemptedVassalizations, factionName)) {
            attemptedVassalizations.add(factionName);
        }
    }

    public boolean hasBeenOfferedVassalization(String factionName) {
        return containsIgnoreCase(attemptedVassalizations, factionName);
    }

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

    public void clearVassals() {
        vassals.clear();
    }

    public int getNumVassals() {
        return vassals.size();
    }

    public String getPrefix() {
        return prefix;
    }

    public void setPrefix(String newPrefix) {
        prefix = newPrefix;
    }

    public ArrayList<String> getVassals() {
        return vassals;
    }

    public boolean isWeakened() {
        return calculateCumulativePowerLevelWithoutVassalContribution() < (getMaximumCumulativePowerLevel() / 2);
    }

    /**
     * Method to automatically handle all data changes when a Faction changes their name.
     * @param oldName of the Faction (dependent).
     * @param newName of the Faction (dependent).
     */
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

    public FactionFlags getFlags() {
        return flags;
    }

    public int getBonusPower() {
        return bonusPower;
    }

    public void setBonusPower(int i) {
        bonusPower = i;
    }

}