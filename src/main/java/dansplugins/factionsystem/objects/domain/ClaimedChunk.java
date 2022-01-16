/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.objects.inherited.Territory;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import preponderous.ponder.misc.Savable;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class ClaimedChunk extends Territory implements Savable {
    private Chunk chunk;
    private String world;

    public ClaimedChunk() {

    }

    public ClaimedChunk(Chunk initialChunk) {
        setChunk(initialChunk);
    }

    public ClaimedChunk(Map<String, String> data){
        this.load(data);
    }

    public void setChunk(Chunk newChunk) {
        chunk = newChunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public double[] getCoordinates() {
        double[] coordinates = new double[2];
        coordinates[0] = chunk.getX();
        coordinates[1] = chunk.getZ();
        return coordinates;
    }

    public void setWorld(String worldName) {
        world = worldName;
    }

    public String getWorld() {
        return world;
    }

    @Override
    public Map<String, String> save() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();

            Map<String, String> saveMap = new HashMap<>();
            saveMap.put("X", gson.toJson(chunk.getX()));
            saveMap.put("Z", gson.toJson(chunk.getZ()));
            saveMap.put("world", gson.toJson(world));
            saveMap.put("holder", gson.toJson(holder));

            return saveMap;
    }

    @Override
    public void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        world = gson.fromJson(data.get("world"), String.class);
        holder = gson.fromJson(data.get("holder"), String.class);

        World chunkWorld = getServer().createWorld(new WorldCreator(world));
        if (chunkWorld != null) {
            chunk = chunkWorld.getChunkAt(gson.fromJson(data.get("X"), Integer.TYPE), gson.fromJson(data.get("Z"), Integer.TYPE));
        }
    }
}