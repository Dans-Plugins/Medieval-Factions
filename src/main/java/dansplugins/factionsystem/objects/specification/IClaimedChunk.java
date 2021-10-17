package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.specification.generic.Territory;
import org.bukkit.Chunk;

public interface IClaimedChunk extends Territory {
    void setChunk(Chunk newChunk);
    Chunk getChunk();
    void setWorld(String worldName);
    String getWorld();
}