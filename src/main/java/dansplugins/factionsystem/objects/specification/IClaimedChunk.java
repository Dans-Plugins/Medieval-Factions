package dansplugins.factionsystem.objects.specification;

import org.bukkit.Chunk;

import java.util.Map;

public interface IClaimedChunk extends Territory {
    void setChunk(Chunk newChunk);
    Chunk getChunk();
    void setWorld(String worldName);
    String getWorld();
}