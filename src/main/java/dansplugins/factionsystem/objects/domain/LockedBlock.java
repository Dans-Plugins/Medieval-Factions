/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import org.bukkit.Bukkit;
import preponderous.ponder.misc.abs.Lockable;
import preponderous.ponder.misc.abs.Savable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;


/**
 * @author Daniel McCoy Stephenson
 */
public class LockedBlock implements Lockable<UUID>, Savable {
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

    public LockedBlock(Map<String, String> lockedBlockData) {
        this.load(lockedBlockData);
    }

    public String getWorld() {
    	return world;
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

    @Override
    public void setOwner(UUID s) {
        owner = s;
    }

    @Override
    public UUID getOwner() {
        return owner;
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

    @Override
    public Map<String, String> save() {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

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

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

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