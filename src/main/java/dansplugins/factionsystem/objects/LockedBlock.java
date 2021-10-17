package dansplugins.factionsystem.objects;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class LockedBlock implements dansplugins.factionsystem.objects.specification.ILockedBlock {

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

    @Override
    public String getWorld() {
    	return world;
    }

    @Override
    public void setWorld(String name) {
    	world = name;
    }

    @Override
    public int getX() {
        return x;
    }

    @Override
    public int getY() {
        return y;
    }

    @Override
    public int getZ() {
        return z;
    }

    @Override
    public void setOwner(UUID s) {
        owner = s;
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public void addToAccessList(UUID playerName) {
        if (!accessList.contains(playerName)) {
            accessList.add(playerName);
        }
    }

    @Override
    public void removeFromAccessList(UUID playerName) {
        accessList.remove(playerName);
    }

    @Override
    public boolean hasAccess(UUID playerName) {
        return accessList.contains(playerName);
    }

    @Override
    public ArrayList<UUID> getAccessList() {
        return accessList;
    }

    public void setFaction(String s) {
        factionName = s;
    }

    public String getFactionName() {
        return factionName;
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

}
