package plugin;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class ClaimedChunk {

    private Chunk chunk;
    private String holder;

    public ClaimedChunk(Chunk initialChunk) {
        setChunk(initialChunk);
    }

    public void setChunk(Chunk newChunk) {
        chunk = newChunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setHolder(String newHolder) {
        holder = newHolder;
    }

    public String getHolder() {
        return holder;
    }

    public double[] getCoordinates() {
        double[] coordinates = new double[2];
        coordinates[0] = chunk.getX();
        coordinates[1] = chunk.getZ();
        return coordinates;
    }

}
