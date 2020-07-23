package factionsystem.Objects;

import com.google.gson.Gson;
import org.bukkit.Chunk;
import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

import static org.bukkit.Bukkit.getServer;

public class ClaimedChunk {

    private Chunk chunk;
    private String holder;
    private String world;

    public ClaimedChunk() {

    }

    public ClaimedChunk(Chunk initialChunk) {
        setChunk(initialChunk);
    }

    public void setChunk(Chunk newChunk) {
        chunk = newChunk;
    }

    public Chunk getChunk() {
        return chunk;
    }

    public void setHolder(String newHolder) {
        holder = newHolder;
    }

    public String getHolder() {
        return holder;
    }

    public double[] getCoordinates() {
        double[] coordinates = new double[2];
        coordinates[0] = chunk.getX();
        coordinates[1] = chunk.getZ();
        return coordinates;
    }

    public void setWorld(String worldName) {
        world = worldName;
    }

    public String getWorld() {
        return world;
    }

    public Map<String, String> save() {
            Gson gson = new Gson();

            Map<String, String> saveMap = new HashMap<>();
            saveMap.put("X", gson.toJson(chunk.getX()));
            saveMap.put("Z", gson.toJson(chunk.getZ()));
            saveMap.put("world", world);
            saveMap.put("holder", holder);

            return saveMap;
    }

    public void legacyLoad(String filename) {
        try {
            File loadFile = new File("./plugins/MedievalFactions/claimedchunks/" + filename);
            Scanner loadReader = new Scanner(loadFile);

            int x = 0;
            int z = 0;

            // actual loading
            if (loadReader.hasNextLine()) {
                x = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                z = Integer.parseInt(loadReader.nextLine());
            }

            if (loadReader.hasNextLine()) {
                world = loadReader.nextLine();
            }

            try {
                System.out.println("Attempting to get chunk...");

                World chunksworld;

                chunksworld = getServer().createWorld(new WorldCreator(world));

                if (chunksworld != null) {
                    chunk = chunksworld.getChunkAt(x, z);

                    if (chunk == null) {
                        System.out.println("Chunk is null!");
                    }
                }
                else  {
                    System.out.println("World is null!");
                }
                System.out.println("Chunk acquired.");
            } catch(Exception e) {
                System.out.println("Failed.");
            }


            if (loadReader.hasNextLine()) {
                setHolder(loadReader.nextLine());
            }

            loadReader.close();

            System.out.println("Claimed chunk " + x + "" + z + " successfully loaded.");

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
        }
    }

}
