package factionsystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class PlayerPowerRecord {
    private String playerName = "";
    private int powerLevel = 0;
    private String playerFaction = "";

    public PlayerPowerRecord(String nameOfPlayer) {
        playerName = nameOfPlayer;
        powerLevel = 10;
    }
    public PlayerPowerRecord() { // server constructor for loading

    }

    public void setPlayerName(String newName) {
        playerName = newName;
    }


    public String getPlayerName() {
        return playerName;
    }

    public void setFaction(String faction) {
        playerFaction = faction;
    }

    public String getFaction() {
        return playerFaction;
    }

    public boolean increasePower() {
        if (powerLevel < 10) {
            powerLevel++;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean decreasePower() {
        if (powerLevel > 0) {
            powerLevel--;
            return true;
        }
        else {
            return false;
        }
    }

    public int getPowerLevel() {
        return powerLevel;
    }

    public void save() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/player-power-records/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/player-power-records/" + playerName + ".txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for claimed chunk " + playerName + " created.");
            } else {
                System.out.println("Save file for claimed chunk " + playerName + " already exists. Altering.");
            }

            FileWriter saveWriter = new FileWriter("./plugins/medievalfactions/player-power-records/" + playerName + ".txt");

            // actual saving takes place here
            saveWriter.write(playerName + "\n");
            saveWriter.write(powerLevel + "\n");
            saveWriter.write(playerFaction + "\n");

            saveWriter.close();

            System.out.println("Successfully saved player power record associated with " + playerName + ".");

        } catch (IOException e) {
            System.out.println("An error occurred saving the player power record associated with " + playerName);
            e.printStackTrace();
        }
    }

    public void load(String filename) {
        try {
            File loadFile = new File("./plugins/medievalfactions/player-power-records/" + filename);
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            if (loadReader.hasNextLine()) {
                playerName = loadReader.nextLine();
            }
            if (loadReader.hasNextLine()) {
                powerLevel = Integer.parseInt(loadReader.nextLine());
            }
            if (loadReader.hasNextLine()) {
                playerFaction = loadReader.nextLine();
            }

            loadReader.close();

            System.out.println("Player power record for " + playerName + " successfully loaded.");

        } catch (FileNotFoundException e) {
            System.out.println("An error occurred loading the file " + filename + ".");
            e.printStackTrace();
        }
    }
}
