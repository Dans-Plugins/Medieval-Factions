package plugin;

import org.bukkit.Chunk;
import org.bukkit.entity.Player;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;

public class ClaimedChunk {

    private Chunk chunk;
    private String holder;

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

    public void save() {

        String identifier = (int)chunk.getX() + "-" + (int)chunk.getZ();

        try {
            File saveFolder = new File("./plugins/medievalfactions/claimedchunks/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/claimedchunks" + identifier + ".txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for claimed chunk " + identifier + " created.");
            } else {
                System.out.println("Save file for claimed chunk " + identifier + " already exists. Altering.");
            }

            FileWriter saveWriter = new FileWriter("./plugins/medievalfactions/claimedchunks" + identifier + ".txt");

            // actual saving takes place here
            saveWriter.write(chunk.getX() + "\n");
            saveWriter.write(chunk.getZ() + "\n");
            saveWriter.write(holder);

            saveWriter.close();

            System.out.println("Successfully saved claimed chunk " + identifier + ".");

        } catch (IOException e) {
            System.out.println("An error occurred saving the claimed chunk with identifier " + identifier);
            e.printStackTrace();
        }
    }

    public void load(String filename) {

    }

}
