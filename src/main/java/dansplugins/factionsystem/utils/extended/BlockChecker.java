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
    private static BlockChecker instance;

    private BlockChecker() {

    }

    public static BlockChecker getInstance() {
        if (instance == null) {
            instance = new BlockChecker();
        }
        return instance;
    }

    public boolean isNextToNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX() + 1, block.getY(), block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX() - 1, block.getY(), block.getZ());
        Block neighbor3 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() + 1);
        Block neighbor4 = block.getWorld().getBlockAt(block.getX(), block.getY(), block.getZ() - 1);

        if (BlockChecker.getInstance().isChest(neighbor1)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor1) && PersistentData.getInstance().getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor2)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor2) && PersistentData.getInstance().getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor3)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor3) && PersistentData.getInstance().getLockedBlock(neighbor3).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor4)) {
            return PersistentData.getInstance().isBlockLocked(neighbor4) && PersistentData.getInstance().getLockedBlock(neighbor4).getOwner() != player.getUniqueId();
        }

        return false;
    }

    public boolean isUnderOrAboveNonOwnedLockedChest(Player player, Block block) {
        // define blocks
        Block neighbor1 = block.getWorld().getBlockAt(block.getX(), block.getY() + 1, block.getZ());
        Block neighbor2 = block.getWorld().getBlockAt(block.getX(), block.getY() - 1, block.getZ());

        if (BlockChecker.getInstance().isChest(neighbor1)) {
            if (PersistentData.getInstance().isBlockLocked(neighbor1) && PersistentData.getInstance().getLockedBlock(neighbor1).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        if (BlockChecker.getInstance().isChest(neighbor2)) {
            return PersistentData.getInstance().isBlockLocked(neighbor2) && PersistentData.getInstance().getLockedBlock(neighbor2).getOwner() != player.getUniqueId();
        }

        return false;
    }
}