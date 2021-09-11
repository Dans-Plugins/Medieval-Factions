package dansplugins.factionsystem.utils;

import dansplugins.factionsystem.data.PersistentData;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.entity.Player;

import static org.bukkit.Bukkit.getLogger;

public class BlockChecker {
    private static BlockChecker instance;

    private BlockChecker() {

    }

    public static BlockChecker getInstance() {
        if (instance == null) {
            instance = new BlockChecker();
        }
        return instance;
    }

    public boolean isChest(Block block) {
        return block.getType() == Material.CHEST && block.getState() instanceof Chest;
    }

    public boolean isDoor(Block block) {
        if (block.getType() == Material.ACACIA_DOOR ||
                block.getType() == Material.BIRCH_DOOR ||
                block.getType() == Material.DARK_OAK_DOOR ||
                block.getType() == Material.IRON_DOOR ||
                block.getType() == Material.JUNGLE_DOOR ||
                block.getType() == Material.OAK_DOOR ||
                block.getType() == Material.SPRUCE_DOOR ||
                block.getType() == compatMaterial("CRIMSON_DOOR") ||
                block.getType() == compatMaterial("WARPED_DOOR")) {

            return true;

        }
        return false;
    }

    public boolean isTrapdoor(Block block) {
        if (block.getType() == Material.IRON_TRAPDOOR ||
                block.getType() == Material.OAK_TRAPDOOR ||
                block.getType() == Material.SPRUCE_TRAPDOOR ||
                block.getType() == Material.BIRCH_TRAPDOOR ||
                block.getType() == Material.JUNGLE_TRAPDOOR ||
                block.getType() == Material.ACACIA_TRAPDOOR ||
                block.getType() == Material.DARK_OAK_TRAPDOOR ||
                block.getType() == compatMaterial("CRIMSON_TRAPDOOR") ||
                block.getType() == compatMaterial("WARPED_TRAPDOOR")) {
            return true;
        }
        return false;
    }

    public boolean isFurnace(Block block) {
        if (block.getType() == Material.FURNACE ||
                block.getType() == Material.BLAST_FURNACE) {
            return true;
        }
        return false;
    }

    public boolean isAnvil(Block block) {
        if (block.getType() == Material.ANVIL ||
                block.getType() == Material.CHIPPED_ANVIL ||
                block.getType() == Material.DAMAGED_ANVIL) {
            return true;
        }
        return false;
    }

    public boolean isGate(Block block) {
        if (block.getType() == Material.OAK_FENCE_GATE ||
                block.getType() == Material.SPRUCE_FENCE_GATE ||
                block.getType() == Material.BIRCH_FENCE_GATE ||
                block.getType() == Material.JUNGLE_FENCE_GATE ||
                block.getType() == Material.ACACIA_FENCE_GATE ||
                block.getType() == Material.DARK_OAK_FENCE_GATE ||
                block.getType() == compatMaterial("CRIMSON_FENCE_GATE") ||
                block.getType() == compatMaterial("WARPED_FENCE_GATE")) {
            return true;
        }
        return false;
    }

    public boolean isBarrel(Block block) {
        if (block.getType() == Material.BARREL) {
            return true;
        }
        return false;
    }

    private Material compatMaterial(String materialName) {
        Material mat = Material.getMaterial(materialName);
        if (mat == null) {
            // Find compatible substitute.
            switch(materialName) {
                case "CRIMSON_FENCE_GATE":
                    return Material.OAK_FENCE_GATE;
                case "WARPED_FENCE_GATE":
                    return Material.OAK_FENCE_GATE;
                case "CRIMSON_DOOR":
                    return Material.OAK_DOOR;
                case "WARPED_DOOR":
                    return Material.OAK_DOOR;
                case "CRIMSON_TRAPDOOR":
                    return Material.OAK_TRAPDOOR;
                case "WARPED_TRAPDOOR":
                    return Material.OAK_TRAPDOOR;
                default:
                    getLogger().info("ERROR: Could not locate a compatible material matching '" + materialName + "'.");
                    return null;
            }
        }
        else {
            return mat;
        }
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
            if (PersistentData.getInstance().isBlockLocked(neighbor4) && PersistentData.getInstance().getLockedBlock(neighbor4).getOwner() != player.getUniqueId()) {
                return true;
            }
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
            if (PersistentData.getInstance().isBlockLocked(neighbor2) && PersistentData.getInstance().getLockedBlock(neighbor2).getOwner() != player.getUniqueId()) {
                return true;
            }
        }

        return false;
    }
}
