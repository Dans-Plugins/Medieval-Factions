/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import org.bukkit.block.Block;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.data.PersistentData;

/**
 * @author Daniel McCoy Stephenson
 * @author Caibinus
 */
public class BlockChecker extends preponderous.ponder.minecraft.bukkit.tools.BlockChecker {

    public boolean isNextToNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (blockChecker.isChest(neighbor1)) {
            if (persistentData.isBlockLocked(neighbor1) && persistentData.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (blockChecker.isChest(neighbor2)) {
            if (persistentData.isBlockLocked(neighbor2) && persistentData.getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (blockChecker.isChest(neighbor3)) {
            if (persistentData.isBlockLocked(neighbor3) && persistentData.getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (blockChecker.isChest(neighbor4)) {
            return persistentData.isBlockLocked(neighbor4) && persistentData.getLockedBlock(neighbor4).getOwner() != player.getUniqueId();
        }

        return false;
    }

    public boolean isUnderOrAboveNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (blockChecker.isChest(neighbor1)) {
            if (persistentData.isBlockLocked(neighbor1) && persistentData.getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (blockChecker.isChest(neighbor2)) {
            return persistentData.isBlockLocked(neighbor2) && persistentData.getLockedBlock(neighbor2).getOwner() != player.getUniqueId();
        }

        return false;
    }
}