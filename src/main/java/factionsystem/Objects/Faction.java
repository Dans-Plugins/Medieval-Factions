package factionsystem.Objects;

import com.google.gson.Gson;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.bukkit.Bukkit.getServer;

public class Faction {

    // saved
    private ArrayList<String> members = new ArrayList<>();
    private ArrayList<String> enemyFactions = new ArrayList<>();
    private ArrayList<String> officers = new ArrayList<>();
    private ArrayList<String> allyFactions = new ArrayList<>();
    private ArrayList<String> laws = new ArrayList<>();
    private String name = "defaultName";
    private String description = "defaultDescription";
    private String owner = "defaultOwner";
    private int cumulativePowerLevel = 0;
    private Location factionHome = null;

    // temporary
    int maxPower = 0;
    private ArrayList<String> invited = new ArrayList<>();
    private ArrayList<String> attemptedTruces = new ArrayList<>();
    private ArrayList<String> attemptedAlliances = new ArrayList<>();
    private boolean autoclaim = false;


    // player constructor
    public Faction(String initialName, String creator, int max) {
        setName(initialName);
        setOwner(creator);
        maxPower = max;
    }

    // server constructor
    public Faction(String initialName, int max) {
        setName(initialName);
        maxPower = max;
    }

    public void addLaw(String newLaw) {
        laws.add(newLaw);
    }

    public boolean removeLaw(String lawToRemove) {
        if (laws.contains(lawToRemove)) {
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
        if (!attemptedTruces.contains(factionName)) {
            attemptedTruces.add(factionName);
        }
    }

    public boolean isTruceRequested(String factionName) {
        for (String faction : attemptedTruces) {
            if (faction.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
    }

    public void removeRequestedTruce(String factionName) {
        attemptedTruces.remove(factionName);
    }

    public void requestAlly(String factionName) {
        if (!attemptedAlliances.contains(factionName)) {
            attemptedAlliances.add(factionName);
        }
    }

    public boolean isRequestedAlly(String factionName) {
        for (String faction : attemptedAlliances) {
            if (faction.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
    }

    public void addAlly(String factionName) {
        allyFactions.add(factionName);
    }

    public void removeAlly(String factionName) {
        allyFactions.remove(factionName);
    }

    public boolean isAlly(String factionName) {
        for (String faction : allyFactions) {
            if (faction.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
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

    public void setCumulativePowerLevel(int newPowerLevel) {
        cumulativePowerLevel = newPowerLevel;
    }

    public int getCumulativePowerLevel() {
        return cumulativePowerLevel;
    }

    public void addPower() {
        if (cumulativePowerLevel < members.size() * maxPower) {
            cumulativePowerLevel++;
        }
    }

    public void addPower(int powerToAdd) {
        if ((cumulativePowerLevel + powerToAdd) < members.size() * maxPower) {
            cumulativePowerLevel = cumulativePowerLevel + powerToAdd;
        }
        else {
            cumulativePowerLevel = members.size() * maxPower;
        }
    }

    public void subtractPower() {
        if (cumulativePowerLevel > 0) {
            cumulativePowerLevel--;
        }
    }

    public void subtractPower(int powerToSubtract) {
        if ((cumulativePowerLevel - powerToSubtract) > 0) {
            cumulativePowerLevel = cumulativePowerLevel - powerToSubtract;
        }
        else {
            subtractPower();
        }
    }

    public void addOfficer(String newOfficer) {
        officers.add(newOfficer);
    }

    public boolean removeOfficer(String officerToRemove) {
        return officers.removeIf(officer -> officer.equalsIgnoreCase(officerToRemove));
    }

    public boolean isOfficer(String playerName) {
        for (String officer : officers) {
            if (officer.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getMemberArrayList() {
        return members;
    }

    public void toggleAutoClaim() {
        autoclaim = !autoclaim;
    }

    public boolean getAutoClaimStatus() {
        return autoclaim;
    }

    public void addEnemy(String factionName) {
        enemyFactions.add(factionName);
    }

    public void removeEnemy(String factionName) {
        enemyFactions.remove(factionName);
    }

    public boolean isEnemy(String factionName) {
        for (String faction : enemyFactions) {
            if (faction.equalsIgnoreCase(factionName)) {
                return true;
            }
        }
        return false;
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

    public void invite(String playerName) {
        invited.add(playerName);
    }

    public void uninvite(String playerName) {
        invited.remove(playerName);
    }

    public boolean isInvited(String playerName) {
        for (String player : invited) {
            if (player.equalsIgnoreCase(playerName)) {
                return true;
            }
        }
        return false;
    }

    public ArrayList<String> getMemberList() {
        ArrayList<String> membersCopy = members;
        return membersCopy;
    }

    public int getPopulation() {
        return members.size();
    }

    public void setOwner(String playerName) {
        owner = playerName;
    }

    public boolean isOwner(String playerName) {
        if (playerName.equalsIgnoreCase(owner)) {
            return true;
        }
        else {
            return false;
        }
    }

    public String getOwner() {
        return owner;
    }

    void setName(String newName) {
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

    public void addMember(String playerName, int power) {
        members.add(playerName);
        cumulativePowerLevel = cumulativePowerLevel + power;
    }

    public void removeMember(String playerName, int power) {
        members.remove(playerName);
        cumulativePowerLevel = cumulativePowerLevel - power;
    }

    public boolean isMember(String playerName) {
        boolean membership = false;
        for (int i = 0; i < members.size(); i++) {
            if (members.get(i).equalsIgnoreCase(playerName)) {
                membership = true;
            }
        }
        return membership;
    }

    public void changeName(String newName) {
        // record old name
        String oldName = name;

        // rename
        name = newName;

        // delete old save file
        System.out.println("Attempting to delete file plugins/MedievalFactions/" + oldName + ".txt");
        try {
            File fileToDelete = new File("plugins/MedievalFactions/" + oldName + ".txt");
            if (fileToDelete.delete()) {
                System.out.println("Success. File deleted.");
            }
            else {
                System.out.println("There was a problem deleting the file.");
            }
        } catch(Exception e) {
            System.out.println("There was a problem encountered during file deletion.");
        }
    }

    public Map<String, String> save() {
        Gson gson = new Gson();
        Map<String, String> saveMap = new HashMap<>();

        saveMap.put("members", gson.toJson(members));
        saveMap.put("enemyFactions", gson.toJson(enemyFactions));
        saveMap.put("officers", gson.toJson(officers));
        saveMap.put("allyFactions", gson.toJson(allyFactions));
        saveMap.put("laws", gson.toJson(laws));
        saveMap.put("name", gson.toJson(name));
        saveMap.put("description", gson.toJson(description));
        saveMap.put("owner", gson.toJson(owner));
        saveMap.put("cumulativePowerLevel", gson.toJson(cumulativePowerLevel));
        saveMap.put("location", gson.toJson(saveLocation(gson)));

        return saveMap;
    }

    private Map<String, String> saveLocation(Gson gson) {
        Map<String, String> saveMap = new HashMap<>();

        saveMap.put("worldName", factionHome.getWorld().getName());
        saveMap.put("x", gson.toJson(factionHome.getX()));
        saveMap.put("y", gson.toJson(factionHome.getY()));
        saveMap.put("z", gson.toJson(factionHome.getZ()));

        return saveMap;
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
                setOwner(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                setDescription(loadReader.nextLine());
            }

            if (loadReader.hasNextLine()) {
                setCumulativePowerLevel(Integer.parseInt(loadReader.nextLine()));
            }

            while (loadReader.hasNextLine()) {
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }

                members.add(temp);
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
                String temp = loadReader.nextLine();

                if (temp.equalsIgnoreCase("-")) {
                    break;
                }

                officers.add(temp);
            }

            String worldname;
            worldname = loadReader.nextLine();
            if (!worldname.equalsIgnoreCase("null")) {

                World world = null;
                double x = 0;
                double y = 0;
                double z = 0;

                try {
                    System.out.println("Attempting to load faction home location for " + name + "...");

                    // load faction home details
                    world = getServer().createWorld(new WorldCreator(worldname));
                    System.out.println("World successfully acquired.");

                    if (loadReader.hasNextLine()) {
//                    System.out.println("Parsing double...");
                        x = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("X position successfully acquired.");
                    }
                    else {
                        System.out.println("X position not found in file!");
                    }
                    if (loadReader.hasNextLine()) {//
                        System.out.println("Parsing double...");
                        y = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("Y position successfully acquired.");
                    }
                    else {
                        System.out.println("Y position not found in file!");
                    }
                    if (loadReader.hasNextLine()) {
                        System.out.println("Parsing double...");
                        z = Double.parseDouble(loadReader.nextLine());
//                    System.out.println("Z position successfully acquired.");
                    }
                    else {
                        System.out.println("Z position not found in file!");
                    }

                    // set location
                    if (world != null && x != 0 && y != 0 && z != 0) {
                        factionHome = new Location(world, x, y, z);
                        System.out.println("Faction home successfully set to " + x + ", " + y + ", " + z + ".");
                    }
                    else {
                        System.out.println("One of the variables the faction home location depends on wasn't loaded!");
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

}