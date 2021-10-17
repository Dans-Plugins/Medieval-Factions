package dansplugins.factionsystem.objects.domain;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.objects.domain.specification.IClaimedChunk;
import dansplugins.factionsystem.objects.inherited.Territory;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.util.HashMap;
import java.util.Map;

import static org.bukkit.Bukkit.getServer;

public class ClaimedChunk extends Territory implements IClaimedChunk {

    private Chunk chunk;
    private String holder;
    private String world;

    public ClaimedChunk() {

    }

    public ClaimedChunk(Chunk initialChunk) {
        setChunk(initialChunk);
    }

    public ClaimedChunk(Map<String, String> data){
        this.load(data);
    }

    @Override
    public void setChunk(Chunk newChunk) {
        chunk = newChunk;
    }

    @Override
    public Chunk getChunk() {
        return chunk;
    }

    @Override
    public void setHolder(String newHolder) {
        holder = newHolder;
    }

    @Override
    public String getHolder() {
        return holder;
    }

    @Override
    public double[] getCoordinates() {
        double[] coordinates = new double[2];
        coordinates[0] = chunk.getX();
        coordinates[1] = chunk.getZ();
        return coordinates;
    }

    @Override
    public void setWorld(String worldName) {
        world = worldName;
    }

    @Override
    public String getWorld() {
        return world;
    }

    public Map<String, String> save() {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();;

            Map<String, String> saveMap = new HashMap<>();
            saveMap.put("X", gson.toJson(chunk.getX()));
            saveMap.put("Z", gson.toJson(chunk.getZ()));
            saveMap.put("world", gson.toJson(world));
            saveMap.put("holder", gson.toJson(holder));

            return saveMap;
    }

    private void load(Map<String, String> data) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();

        world = gson.fromJson(data.get("world"), String.class);
        holder = gson.fromJson(data.get("holder"), String.class);

        World chunkWorld = getServer().createWorld(new WorldCreator(world));
        if (chunkWorld != null) {
            chunk = chunkWorld.getChunkAt(gson.fromJson(data.get("X"), Integer.TYPE),
                                            gson.fromJson(data.get("Z"), Integer.TYPE));
        }
    }

}
