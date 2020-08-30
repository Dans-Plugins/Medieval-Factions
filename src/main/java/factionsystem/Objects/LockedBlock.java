package factionsystem.Objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import org.bukkit.Bukkit;

import static factionsystem.Subsystems.UtilitySubsystem.findUUIDBasedOnPlayerName;

public class LockedBlock {

    private int x = 0;
    private int y = 0;
    private int z = 0;
    private UUID owner = UUID.randomUUID();
    private String factionName = "";
    private String world = "";
    private ArrayList<UUID> accessList = new ArrayList<>();

    public LockedBlock(UUID o, String f, int newX, int newY, int newZ, String newW) {
        owner = o;
        factionName = f;
        x = newX;
        y = newY;
        z = newZ;
        world = newW;
        accessList.add(owner);
    }

    public LockedBlock() {
        // server constructor
    }

    public LockedBlock(Map<String, String> lockedBlockData) {
        this.load(lockedBlockData);
    }

    public String getWorld() {
    	return world;
    }
    
    public void setWorld(String name) {
    	world = name;
    }
    
    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void setOwner(UUID s) {
        owner = s;
    }

    public UUID getOwner() {
        return owner;
    }

    public void setFaction(String s) {
        factionName = s;
    }

    public String getFactionName() {
        return factionName;
    }

    public void addToAccessList(UUID playerName) {
        if (!accessList.contains(playerName)) {
            accessList.add(playerName);
        }
    }

    public void removeFromAccessList(UUID playerName) {
        accessList.remove(playerName);
    }

    public boolean hasAccess(UUID playerName) {
        return accessList.contains(playerName);
    }

    public ArrayList<UUID> getAccessList() {
        return accessList;
    }

    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;

        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("X", gson.toJson(x));
        saveMap.put("Y", gson.toJson(y));
        saveMap.put("Z", gson.toJson(z));
        saveMap.put("owner", gson.toJson(owner));
        saveMap.put("factionName", gson.toJson(factionName));
        saveMap.put("world", gson.toJson(world));
        saveMap.put("accessList", gson.toJson(accessList));

        return saveMap;
    }

    private void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();;

        x = gson.fromJson(data.get("X"), Integer.TYPE);
        y = gson.fromJson(data.get("Y"), Integer.TYPE);
        z = gson.fromJson(data.get("Z"), Integer.TYPE);
        owner = UUID.fromString(gson.fromJson(data.get("owner"), String.class));
        factionName =  gson.fromJson(data.get("factionName"), String.class);
        world = gson.fromJson(data.get("world"), String.class);
        if (world == null)
        {
        	world = Bukkit.getServer().getWorlds().get(0).getName();
        }
        accessList = gson.fromJson(data.get("accessList"), new TypeToken<ArrayList<UUID>>(){}.getType());
        
    }

    public void legacyLoad(String filename) {
        try {
            File loadFile = new File("./plugins/MedievalFactions/lockedblocks/" + filename);
            Scanner loadReader = new Scanner(loadFile);


            // actual loading
            if (loadReader.hasNextLine()) {
                x = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                y = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                z = Integer.parseInt(loadReader.nextLine());
            }

            // owner
            if (loadReader.hasNextLine()) {
                owner = findUUIDBasedOnPlayerName(loadReader.nextLine());
            }

            // faction name
            if (loadReader.hasNextLine()) {
                factionName = loadReader.nextLine();
            }

            // access list
            while (loadReader.hasNextLine()) {
                accessList.add(findUUIDBasedOnPlayerName(loadReader.nextLine()));
            }

            loadReader.close();

            System.out.println("Locked block " + x + "_" + y + "_" + z + " successfully loaded.");

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
        }
    }

}
