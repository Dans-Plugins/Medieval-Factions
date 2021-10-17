package dansplugins.factionsystem.objects.domain.specification;

import dansplugins.factionsystem.objects.inherited.specification.ITerritory;
import org.bukkit.Chunk;

public interface IClaimedChunk {
    void setChunk(Chunk newChunk);
    Chunk getChunk();
    void setWorld(String worldName);
    String getWorld();
}