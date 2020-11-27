package factionsystem.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import factionsystem.Main;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileNotFoundException;
import java.lang.reflect.Type;
import java.util.*;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;
import static factionsystem.Subsystems.UtilitySubsystem.getPlayersPowerRecord;
import static org.bukkit.Bukkit.getServer;

public class Faction {

    // saved
    private ArrayList<UUID> members = new ArrayList<>();
    private ArrayList<String> enemyFactions = new ArrayList<>();
    private ArrayList<UUID> officers = new ArrayList<>();
    private ArrayList<String> allyFactions = new ArrayList<>();
    private ArrayList<String> laws = new ArrayList<>();
    private ArrayList<String> vassals = new ArrayList<>();
    private String name = "defaultName";
    private String description = "defaultDescription";
    private String liege = "none";
    private UUID owner = UUID.randomUUID();
    private int cumulativePowerLevel = 0; // I'm not sure this variable is needed anymore... -Dan 11/27/2020
    private Location factionHome = null;
    private ArrayList<Gate> gates = new ArrayList<>();

    // temporary
    int maxPower = 0;
    private ArrayList<UUID> invited = new ArrayList<>();
    private ArrayList<String> attemptedTruces = new ArrayList<>();
    private ArrayList<String> attemptedAlliances = new ArrayList<>();
    private ArrayList<String> attemptedVassalizations = new ArrayList<>();
    private boolean autoclaim = false;
    private Main main;


    // player constructor
    public Faction(String initialName, UUID creator, int max, Main main) {
        setName(initialName);
        setOwner(creator);
        maxPower = max;
        this.main = main;
    }

    // server constructor
    public Faction(String initialName, int max, Main main) {
        setName(initialName);
        maxPower = max;
        this.main = main;
    }

    public ArrayList<Gate> getGates()
    {
    	return gates;
    }    
    
    // Must recieve json data
    public Faction(Map<String, String> data, Main main) {
        this.main = main;
        this.load(data);
    }

    public int getNumOfficers() {
        return officers.size();
    }

    public void addLaw(String newLaw) {
        laws.add(newLaw);
    }

    public boolean removeLaw(String lawToRemove) {
        if (main.utilities.containsIgnoreCase(laws, lawToRemove)) {
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
        if (!main.utilities.containsIgnoreCase(attemptedTruces, factionName)) {
            attemptedTruces.add(factionName);
        }
    }

    public boolean isTruceRequested(String factionName) {
        return main.utilities.containsIgnoreCase(attemptedTruces, factionName);
    }

    public void removeRequestedTruce(String factionName) {
        main.utilities.removeIfContainsIgnoreCase(attemptedTruces, factionName);
    }

    public void requestAlly(String factionName) {
        if (!main.utilities.containsIgnoreCase(attemptedAlliances, factionName)) {
            attemptedAlliances.add(factionName);
        }
    }

    public boolean isRequestedAlly(String factionName) {
        return main.utilities.containsIgnoreCase(attemptedAlliances, factionName);
    }

    public void addAlly(String factionName) {
        if (!main.utilities.containsIgnoreCase(allyFactions, factionName)) {
            allyFactions.add(factionName);
        }
    }

    public void removeAlly(String factionName) {
        main.utilities.removeIfContainsIgnoreCase(allyFactions, factionName);
    }

    public boolean isAlly(String factionName) {
        return main.utilities.containsIgnoreCase(allyFactions, factionName);
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
        int powerLevel = 0;
        for (UUID playerUUID : members){
            try
            {
            	powerLevel += getPlayersPowerRecord(playerUUID, main.playerPowerRecords).getPowerLevel();
            }
            catch (Exception e)
            {
            	System.out.println("ERROR: Player's Power Record for uuid " + playerUUID + " not found. Could not get cumulative power level.");
            }
        }
        return powerLevel;
    }

    public int calculateMaxOfficers(){
        int officersPerXNumber = main.getConfig().getInt("officerPerMemberCount");
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
        if (!main.utilities.containsIgnoreCase(enemyFactions, factionName)) {
            enemyFactions.add(factionName);
        }
    }

    public void removeEnemy(String factionName) {
        main.utilities.removeIfContainsIgnoreCase(enemyFactions, factionName);
    }

    public boolean isEnemy(String factionName) {
        return main.utilities.containsIgnoreCase(enemyFactions, factionName);
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

    public void addMember(UUID UUID, int power) {
        members.add(UUID);
        cumulativePowerLevel = cumulativePowerLevel + power;
    }

    public void removeMember(UUID UUID, int power) {
        members.remove(UUID);
        cumulativePowerLevel = cumulativePowerLevel - power;
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
        saveMap.put("cumulativePowerLevel", gson.toJson(cumulativePowerLevel));
        saveMap.put("location", gson.toJson(saveLocation(gson)));
        saveMap.put("liege", gson.toJson(liege));

        ArrayList<String> gateList = new ArrayList<String>(); 
        for (Gate gate : gates)
        {
        	Map <String, String> map = gate.save();
        	gateList.add(gson.toJson(map));
        }
        saveMap.put("factionGates", gson.toJson(gateList));        
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
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;

        Type arrayListTypeString = new TypeToken<ArrayList<String>>(){}.getType();
        Type arrayListTypeUUID = new TypeToken<ArrayList<UUID>>(){}.getType();
        Type mapType = new TypeToken<HashMap<String, String>>(){}.getType();

        members = gson.fromJson(data.get("members"), arrayListTypeUUID);
        enemyFactions = gson.fromJson(data.get("enemyFactions"), arrayListTypeString);
        officers = gson.fromJson(data.get("officers"), arrayListTypeUUID);
        allyFactions = gson.fromJson(data.get("allyFactions"), arrayListTypeString);
        laws = gson.fromJson(data.get("laws"), arrayListTypeString);
        name = gson.fromJson(data.get("name"), String.class);
        description = gson.fromJson(data.get("description"), String.class);
        owner = UUID.fromString(gson.fromJson(data.get("owner"), String.class));
        cumulativePowerLevel = gson.fromJson(data.get("cumulativePowerLevel"), Integer.TYPE);
        factionHome = loadLocation(gson.fromJson(data.get("location"), mapType), gson);
        liege = gson.fromJson(data.getOrDefault("liege", "none"), String.class);
        vassals = gson.fromJson(data.getOrDefault("vassals", "[]"), arrayListTypeString);
        
//        System.out.println("Loading Faction Gates...");
        ArrayList<String> gateList = new ArrayList<String>();
        gateList = gson.fromJson(data.get("factionGates"), arrayListTypeString);
        if (gateList != null)
        {
	        for (String item : gateList)
	        {
	        	Gate g = Gate.load(item, main);
	        	gates.add(g);
	        }
        }
        else
        {
        	System.out.println("Could not load gates because the collection 'factionGates' did not exist in the factions JSON file. Are you upgrading from a previous version? Setting default.");
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

    public boolean legacyLoad(String filename) {
        try {
            File loadFile = new File("./plugins/MedievalFactions/" + filename);
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            if (loadReader.hasNextLine()) {
                setName(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                String playerName = loadReader.nextLine();
                setOwner(findUUIDBasedOnPlayerName(playerName));
            }
            if (loadReader.hasNextLine()) {
                setDescription(loadReader.nextLine());
            }

            if (loadReader.hasNextLine()) {
                // Read legacy line and move along across Cumulative Power Record.
                loadReader.nextLine();
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }
                members.add(findUUIDBasedOnPlayerName(temp));
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }

                enemyFactions.add(temp);
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }

                allyFactions.add(temp);
            }

            while (loadReader.hasNextLine()) {
                String playerName = loadReader.nextLine();

                if (playerName.equalsIgnoreCase("-")) {
                    break;
                }

                officers.add(findUUIDBasedOnPlayerName(playerName));
            }

            String worldname;
            worldname = loadReader.nextLine();
            if (!worldname.equalsIgnoreCase("null")) {

                World world = null;
                double x = 0;
                double y = 0;
                double z = 0;

                try {
//                    System.out.println("Attempting to load faction home location for " + name + "...");

                    // load faction home details
                    world = getServer().createWorld(new WorldCreator(worldname));
//                    System.out.println("World successfully acquired.");

                    if (loadReader.hasNextLine()) {
//                    System.out.println("Parsing double...");
                        x = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("X position successfully acquired.");
                    }
                    else {
//                        System.out.println("X position not found in file!");
                    }
                    if (loadReader.hasNextLine()) {//
//                        System.out.println("Parsing double...");
                        y = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("Y position successfully acquired.");
                    }
                    else {
//                        System.out.println("Y position not found in file!");
                    }
                    if (loadReader.hasNextLine()) {
//                        System.out.println("Parsing double...");
                        z = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("Z position successfully acquired.");
                    }
                    else {
//                        System.out.println("Z position not found in file!");
                    }

                    // set location
                    if (world != null && x != 0 && y != 0 && z != 0) {
                        factionHome = new Location(world, x, y, z);
//                        System.out.println("Faction home successfully set to " + x + ", " + y + ", " + z + ".");
                    }
                    else {
//                        System.out.println("One of the variables the faction home location depends on wasn't loaded!");
                    }

                }
                catch(Exception e) {
                    System.out.println("An error occurred loading the faction home position.");
                }
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }

                laws.add(temp);
            }

            loadReader.close();
            System.out.println("Faction " + name + " successfully loaded.");
            return true;

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
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
                ", cumulativePowerLevel=" + cumulativePowerLevel +
                ", liege=" + liege +
                '}';
    }

    public boolean isVassal(String faction) {
        return(main.utilities.containsIgnoreCase(vassals, faction));
    }

    public boolean hasLiege() {
        return !liege.equalsIgnoreCase("none");
    }

    public boolean isLiege(String faction) {
        return liege.equalsIgnoreCase(faction);
    }

    public void addVassal(String factionName) {
        if (!main.utilities.containsIgnoreCase(vassals, factionName)) {
            vassals.add(factionName);
        }
    }

    public void removeVassal(String faction) {
        main.utilities.removeIfContainsIgnoreCase(vassals, faction);
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
        if (!main.utilities.containsIgnoreCase(attemptedVassalizations, factionName)) {
            attemptedVassalizations.add(factionName);
        }
    }

    public boolean hasBeenOfferedVassalization(String factionName) {
        return main.utilities.containsIgnoreCase(attemptedVassalizations, factionName);
    }

    public void removeAttemptedVassalization(String factionName) {
        main.utilities.removeIfContainsIgnoreCase(attemptedVassalizations, factionName);
    }
}