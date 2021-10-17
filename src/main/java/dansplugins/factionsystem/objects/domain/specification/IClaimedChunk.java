package dansplugins.factionsystem.objects.domain.specification;

import org.bukkit.Chunk;

public interface IClaimedChunk {
    void setChunk(Chunk newChunk);
    Chunk getChunk();
    void setWorld(String worldName);
    String getWorld();
}