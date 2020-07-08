package factionsystem.subsystems;

import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Scanner;

public class StorageSubsystem {

    Main main = null;

    public StorageSubsystem(Main plugin) {
        main = plugin;
    }

    public void save() {
        saveFactionNames();
        saveFactions();
        saveClaimedChunkFilenames();
        saveClaimedChunks();
        savePlayerPowerRecordFilenames();
        savePlayerPowerRecords();
        saveLockedBlockFilenames();
        saveLockedBlocks();
    }

    public void saveFactionNames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/" + "faction-names.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for faction names created.");
            } else {
                System.out.println("Save file for faction names already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (Faction faction : main.factions) {
                saveWriter.write(faction.getName() + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving faction names.");
        }
    }

    public void saveFactions() {
        System.out.println("Saving factions...");
        for (Faction faction : main.factions) {
            faction.save(main.factions);
        }
        System.out.println("Factions saved.");
    }

    public void saveClaimedChunkFilenames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/claimedchunks/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/claimedchunks/" + "claimedchunks.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for claimed chunk filenames created.");
            } else {
                System.out.println("Save file for claimed chunk filenames already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (ClaimedChunk chunk : main.claimedChunks) {
                double[] coords = chunk.getCoordinates();

                saveWriter.write((int)coords[0] + "_" + (int)coords[1] + ".txt" + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving claimed chunk filenames.");
        }
    }

    public void saveClaimedChunks() {
        System.out.println("Saving claimed chunks...");
        for (ClaimedChunk chunk : main.claimedChunks) {
            chunk.save();
        }
        System.out.println("Claimed chunks saved.");
    }

    public void savePlayerPowerRecordFilenames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/player-power-records/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/player-power-records/" + "playerpowerrecords.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for player power record filenames created.");
            } else {
                System.out.println("Save file for player power record filenames already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (PlayerPowerRecord record : main.playerPowerRecords) {
                saveWriter.write(record.getPlayerName() + ".txt" + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving player power record filenames.");
        }
    }

    public void savePlayerPowerRecords() {
        System.out.println("Saving player power records...");
        for (PlayerPowerRecord record: main.playerPowerRecords) {
            record.save();
        }
        System.out.println("Player power records saved.");
    }

    public void saveLockedBlockFilenames() {
        try {
            File saveFolder = new File("./plugins/medievalfactions/lockedblocks/");
            if (!saveFolder.exists()) {
                saveFolder.mkdir();
            }
            File saveFile = new File("./plugins/medievalfactions/lockedblocks/" + "lockedblocks.txt");
            if (saveFile.createNewFile()) {
                System.out.println("Save file for locked block filenames created.");
            } else {
                System.out.println("Save file for locked block filenames already exists. Overwriting.");
            }

            FileWriter saveWriter = new FileWriter(saveFile);

            // actual saving takes place here
            for (LockedBlock block : main.lockedBlocks) {
                saveWriter.write(block.getX() + "_" + block.getY() + "_" + block.getZ() + ".txt" + "\n");
            }

            saveWriter.close();

        } catch (IOException e) {
            System.out.println("An error occurred while saving locked block filenames.");
        }
    }

    public void saveLockedBlocks() {
        for (LockedBlock block : main.lockedBlocks) {
            block.save();
        }
    }

    public void load() {
        loadFactions();
        loadClaimedChunks();
        loadPlayerPowerRecords();
        loadLockedBlocks();
    }

    public void loadFactions() {
        try {
            System.out.println("Attempting to load factions...");
            File loadFile = new File("./plugins/medievalfactions/" + "faction-names.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                Faction temp = new Faction(nextName); // uses server constructor, only temporary
                temp.load(nextName + ".txt"); // provides owner field among other things

                // existence check
                for (int i = 0; i < main.factions.size(); i++) {
                    if (main.factions.get(i).getName().equalsIgnoreCase(temp.getName())) {
                        main.factions.remove(i);
                        break;
                    }
                }

                main.factions.add(temp);

            }

            loadReader.close();
            System.out.println("Factions successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("There was a problem loading the factions!");
        }
    }

    public void loadClaimedChunks() {
        System.out.println("Loading claimed chunks...");

        try {
            System.out.println("Attempting to load claimed chunks...");
            File loadFile = new File("./plugins/medievalfactions/claimedchunks/" + "claimedchunks.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                ClaimedChunk temp = new ClaimedChunk(); // uses no-parameter constructor since load provides chunk
                temp.load(nextName); // provides owner field among other things

                // existence check
                for (int i = 0; i < main.claimedChunks.size(); i++) {
                    if (main.claimedChunks.get(i).getChunk().getX() == temp.getChunk().getX() &&
                            main.claimedChunks.get(i).getChunk().getZ() == temp.getChunk().getZ()) {
                        main.claimedChunks.remove(i);
                        break;
                    }
                }

                main.claimedChunks.add(temp);

            }

            loadReader.close();
            System.out.println("Claimed chunks successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("There was a problem loading the claimed chunks!");
        }

        System.out.println("Claimed chunks loaded.");
    }

    public void loadPlayerPowerRecords() {
        System.out.println("Loading player power records...");

        try {
            System.out.println("Attempting to load player power record filenames...");
            File loadFile = new File("./plugins/medievalfactions/player-power-records/" + "playerpowerrecords.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                PlayerPowerRecord temp = new PlayerPowerRecord(); // uses no-parameter constructor since load provides name
                temp.load(nextName); // provides power field among other things

                for (int i = 0; i < main.playerPowerRecords.size(); i++) {
                    if (main.playerPowerRecords.get(i).getPlayerName().equalsIgnoreCase(temp.getPlayerName())) {
                        main.playerPowerRecords.remove(i);
                        break;
                    }
                }

                main.playerPowerRecords.add(temp);
            }

            loadReader.close();
            System.out.println("Player power records loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("There was a problem loading the player power records!");
        }

        System.out.println("Player power records loaded.");
    }

    public void loadLockedBlocks() {
        System.out.println("Loading locked blocks...");

        try {
            System.out.println("Attempting to load locked blocks...");
            File loadFile = new File("./plugins/medievalfactions/lockedblocks/" + "lockedblocks.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                LockedBlock temp = new LockedBlock(); // uses no-parameter constructor since load provides chunk
                temp.load(nextName);

                // existence check
                for (int i = 0; i < main.lockedBlocks.size(); i++) {
                    if (main.lockedBlocks.get(i).getX() == temp.getX() && main.lockedBlocks.get(i).getY() == temp.getY() && main.lockedBlocks.get(i).getZ() == temp.getZ()) {
                        main.lockedBlocks.remove(i);
                    }
                }

                main.lockedBlocks.add(temp);

            }

            loadReader.close();
            System.out.println("Claimed chunks successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("There was a problem loading the claimed chunks!");
        }

        System.out.println("Claimed chunks loaded.");
    }

}
