/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.FontMetrics;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.Chunk;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * @author Callum Johnson
 */
public class MapCommand extends SubCommand {
    private final char[] map_keys = "\\/#$%=&^ABCDEFGHJKLMNOPQRSTUVWXYZ0123456789abcdeghjmnopqrsuvwxyz?".toCharArray();

    public MapCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "map", "showmap", "displaymap"
        }, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.map";
        if (!(checkPermissions(player, permission))) {
            return;
        }
        final Chunk center = player.getLocation().getChunk();
        // Needs to be Odd.
        int map_width = 53;
        final int topLeftX = center.getX() - (map_width / 2);
        // Needs to be Odd.
        int map_height = 13;
        final int topLeftZ = center.getZ() - (map_height / 2);
        final int bottomRightX = center.getX() + (map_width / 2);
        final int bottomRightZ = center.getZ() + (map_height / 2);
        final Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
        final boolean hasFaction = faction != null;
        final HashMap<String, Integer> printedHolders = new HashMap<>();
        final HashMap<String, String> colourMap = new HashMap<>();
        player.sendMessage(FontMetrics.obtainCenteredMessage("&fNorth"));
        for (int z = topLeftZ; z <= bottomRightZ; z++) {
            final StringBuilder line = new StringBuilder();
            for (int x = topLeftX; x <= bottomRightX; x++) {
                Chunk tmp = center.getWorld().getChunkAt(x, z);
                if (chunkDataAccessor.isClaimed(tmp)) {
                    ClaimedChunk chunk = chunkDataAccessor.getClaimedChunk(tmp);
                    printedHolders.put(chunk.getHolder(), printedHolders.getOrDefault(chunk.getHolder(), 0) + 1);
                    int index = getIndex(chunk.getHolder(), printedHolders);
                    char map_key = index == -1 ? '§' : map_keys[index];
                    if (hasFaction) {
                        String colour;
                        if (chunk.getChunk().equals(center)) {
                            colour = "&5"; // If the current position is the player-position, make it purple.
                            map_key = '+';
                            printedHolders.put(chunk.getHolder(), printedHolders.get(chunk.getHolder()) - 1);
                        } else if (chunk.getHolder().equals(faction.getName())) {
                            colour = "&a"; // If the faction is the player-faction, make it green.
                            map_key = '+';
                        } else if (faction.isEnemy(chunk.getHolder())) {
                            colour = "&c"; // If they are an enemy to the player-faction, make it red.
                            colourMap.put(chunk.getHolder(), "&c");
                        } else if (faction.isAlly(chunk.getHolder())) {
                            colour = "&b"; // If they are an ally to the player-faction, make it blue.
                            colourMap.put(chunk.getHolder(), "&b");
                        } else {
                            colour = "&f"; // Default to White.
                            colourMap.put(chunk.getHolder(), "&f");
                        }
                        line.append(colour);
                    } else {
                        line.append("&c"); // Always default to Enemy.
                    }
                    line.append(map_key);
                } else {
                    if (tmp.equals(center)) {
                        line.append("&5+"); // If the current position is the player-position, make it purple.
                    } else {
                        line.append("&7-"); // Gray for no Faction.
                    }
                }
            }
            player.sendMessage(translate(line.toString()));
        }
        player.sendMessage(translate(" &5+&7 = You"));
        final List<String> added = new ArrayList<>();
        int index = 0;
        for (String printedHolder : printedHolders.keySet()) {
            if (!(printedHolders.get(printedHolder) <= 0)) {
                String line;
                try {
                    if (hasFaction && printedHolder.equals(faction.getName())) {
                        line = "&a+&7 = " + printedHolder;
                    } else {
                        if (hasFaction) {
                            line = colourMap.get(printedHolder) + map_keys[index] + "&7 = " + printedHolder;
                        } else {
                            line = "&c" + map_keys[index] + "&7 = " + printedHolder;
                        }
                    }
                } catch (IndexOutOfBoundsException ex) {
                    line = "&7§ = " + printedHolder;
                }
                added.add(line);
            }
            index++;
        }
        if (!added.isEmpty()) { // We don't wanna send an empty line, so check if the added lines is empty or not.
            player.sendMessage(" " + translate(String.join(", ", added)));
        }
    }

    /**
     * Method to obtain the index of a key in a hashmap.
     *
     * @param holder         or key.
     * @param printedHolders hashmap.
     * @return integer index or {@code -1}.
     */
    private int getIndex(String holder, HashMap<String, Integer> printedHolders) {
        return new ArrayList<>(printedHolders.keySet()).indexOf(holder);
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }
}