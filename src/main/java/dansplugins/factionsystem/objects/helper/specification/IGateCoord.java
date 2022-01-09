package dansplugins.factionsystem.objects.helper.specification;

import org.bukkit.block.Block;

/**
 * @author Daniel McCoy Stephenson
 */
public interface IGateCoord {
    String getWorld();
    int getX();
    int getY();
    int getZ();
    boolean equals(Block block);
}