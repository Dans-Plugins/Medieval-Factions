package factionsystem.Objects;

import org.bukkit.World;
import org.bukkit.WorldCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

import static org.bukkit.Bukkit.getServer;

public class LockedBlock {

    private int x = 0;
    private int y = 0;
    private int z = 0;
    private String owner = "";
    private String factionName = "";
    private ArrayList<String> accessList = new ArrayList<>();

    public LockedBlock(String o, String f, int newX, int newY, int newZ) {
        owner = o;
        factionName = f;
        x = newX;
        y = newY;
        z = newZ;
        accessList.add(owner);
    }

    public LockedBlock() {
        // server constructor
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

    public void setOwner(String s) {
        owner = s;
    }

    public String getOwner() {
        return owner;
    }

    public void setFaction(String s) {
        factionName = s;
    }

    public String getFactionName() {
        return factionName;
    }

    public void addToAccessList(String playerName) {
        if (!accessList.contains(playerName)) {
            accessList.add(playerName);
        }
    }

    public void removeFromAccessList(String playerName) {
        if (accessList.contains(playerName)) {
            accessList.remove(playerName);
        }
    }

    public boolean hasAccess(String playerName) {
        return accessList.contains(playerName);
    }

    public ArrayList<String> getAccessList() {
        return accessList;
    }

    public void save() {

        String identifier = x + "_" + y + "_" + z;

        try {
            File saveFolder = new File("./plugins/MedievalFactions/lockedblocks/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/MedievalFactions/lockedblocks/" + identifier + ".txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for locked block " + identifier + " created.");
            } else {
                System.out.println("Save file for locked block " + identifier + " already exists. Altering.");
            }

            FileWriter saveWriter = new FileWriter("./plugins/MedievalFactions/lockedblocks/" + identifier + ".txt");

            // actual saving takes place here
            saveWriter.write(x + "\n");
            saveWriter.write(y + "\n");
            saveWriter.write(z + "\n");

            saveWriter.write(owner + "\n");
            saveWriter.write(factionName + "\n");

            for (String playerName : accessList) {
                saveWriter.write(playerName + "\n");
            }

            saveWriter.close();

            System.out.println("Successfully saved locked block " + identifier + ".");

        } catch (IOException e) {
            System.out.println("An error occurred saving the locked block with identifier " + identifier);
        }
    }

    public void load(String filename) {
        try {
            File loadFile = new File("./plugins/MedievalFactions/lockedblocks/" + filename);
            Scanner loadReader = new Scanner(loadFile);


            // actual loading
            if (loadReader.hasNextLine()) {
                x = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                y = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                z = Integer.parseInt(loadReader.nextLine());
            }

            // owner
            if (loadReader.hasNextLine()) {
                owner = loadReader.nextLine();
            }

            // faction name
            if (loadReader.hasNextLine()) {
                factionName = loadReader.nextLine();
            }

            // access list
            while (loadReader.hasNextLine()) {
                accessList.add(loadReader.nextLine());
            }

            loadReader.close();

            System.out.println("Locked block " + x + "_" + y + "_" + z + " successfully loaded.");

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
        }
    }

}
