package dansplugins.factionsystem.utils;

import org.bukkit.Material;
import org.bukkit.block.Block;

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
        return block.getType() == Material.CHEST;
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

    public boolean isTrapdoor(Block block)
    {
        if (block.getType() == Material.IRON_TRAPDOOR ||
                block.getType() == Material.OAK_TRAPDOOR ||
                block.getType() == Material.SPRUCE_TRAPDOOR ||
                block.getType() == Material.BIRCH_TRAPDOOR ||
                block.getType() == Material.JUNGLE_TRAPDOOR ||
                block.getType() == Material.ACACIA_TRAPDOOR ||
                block.getType() == Material.DARK_OAK_TRAPDOOR ||
                block.getType() == compatMaterial("CRIMSON_TRAPDOOR") ||
                block.getType() == compatMaterial("WARPED_TRAPDOOR"))
        {
            return true;
        }
        return false;
    }

    public boolean isFurnace(Block block)
    {
        if (block.getType() == Material.FURNACE ||
                block.getType() == Material.BLAST_FURNACE)
        {
            return true;
        }
        return false;
    }

    public boolean isAnvil(Block block)
    {
        if (block.getType() == Material.ANVIL ||
                block.getType() == Material.CHIPPED_ANVIL ||
                block.getType() == Material.DAMAGED_ANVIL)
        {
            return true;
        }
        return false;
    }

    public boolean isGate(Block block)
    {
        if (block.getType() == Material.OAK_FENCE_GATE ||
                block.getType() == Material.SPRUCE_FENCE_GATE ||
                block.getType() == Material.BIRCH_FENCE_GATE ||
                block.getType() == Material.JUNGLE_FENCE_GATE ||
                block.getType() == Material.ACACIA_FENCE_GATE ||
                block.getType() == Material.DARK_OAK_FENCE_GATE ||
                block.getType() == compatMaterial("CRIMSON_FENCE_GATE") ||
                block.getType() == compatMaterial("WARPED_FENCE_GATE"))
        {
            return true;
        }
        return false;
    }

    public boolean isBarrel(Block block)
    {
        if (block.getType() == Material.BARREL)
        {
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
}
