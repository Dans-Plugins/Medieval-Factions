package factionsystem.Objects;

import com.google.gson.Gson;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

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

    public Map<String, String> save() {
        Gson gson = new Gson();

        Map<String, String> saveMap = new HashMap<>();
        saveMap.put("X", gson.toJson(x));
        saveMap.put("Y", gson.toJson(y));
        saveMap.put("Z", gson.toJson(z));
        saveMap.put("owner", owner);
        saveMap.put("factioName", factionName);
        saveMap.put("accessList", gson.toJson(accessList));

        return saveMap;
    }

    public void legacyLoad(String filename) {
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
