package factionsystem.Subsystems;

import com.google.gson.Gson;
import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class StorageSubsystem {

    Main main = null;

    private final static String FILE_PATH = "./plugins/MedievalFactions/";
    private final static String LEGACY_FACTIONS_FILE_NAME = "faction-names.txt";
    private final static String LEGACY_CHUNKS_FILE_NAME = "claimedchunks/claimedchunks.txt";
    private final static String LEGACY_PLAYERPOWER_FILE_NAME = "player-power-records/playerpowerrecords.txt";
    private final static String LEGACY_LOCKED_BLOCKS_FILE_NAME = "lockedblocks/lockedblocks.txt";
    private final static String FACTIONS_FILE_NAME = "factions.json";
    private final static String CHUNKS_FILE_NAME = "claimedchunks.json";
    private final static String PLAYERPOWER_FILE_NAME = "playerpowerrecords.json";
    private final static String LOCKED_BLOCKS_FILE_NAME = "lockedblocks.json";

    private Gson gson = new Gson();



    public StorageSubsystem(Main plugin) {
        main = plugin;
    }

    public void save() {
        saveFactions();
        saveClaimedChunks();
        savePlayerPowerRecords();
        saveLockedBlocks();
    }

    private void saveFactions() {
        System.out.println("Saving factions...");

        List<Map<String, String>> factions = new ArrayList<>();
        for (Faction faction : main.factions){
            factions.add(faction.save());
        }

        File file = new File(FILE_PATH + FACTIONS_FILE_NAME);
        writeOutFiles(file, factions);

        System.out.println("Factions saved.");
    }

    private void saveClaimedChunks() {
        System.out.println("Saving Claimed Chunks...");

        List<Map<String, String>> chunks = new ArrayList<>();
        for (ClaimedChunk chunk : main.claimedChunks){
            chunks.add(chunk.save());
        }

        File file = new File(FILE_PATH + CHUNKS_FILE_NAME);
        writeOutFiles(file, chunks);

        System.out.println("Claimed Chunks saved.");
    }

    private void savePlayerPowerRecords() {
        System.out.println("Saving player power records...");

        List<Map<String, String>> playerPowerRecords = new ArrayList<>();
        for (PlayerPowerRecord record : main.playerPowerRecords){
            playerPowerRecords.add(record.save());
        }

        File factionFile = new File(FILE_PATH + PLAYERPOWER_FILE_NAME);
        writeOutFiles(factionFile, playerPowerRecords);

        System.out.println("player power records saved.");
    }

    private void saveLockedBlocks() {
        System.out.println("Saving locked blocks...");

        List<Map<String, String>> lockedBlocks = new ArrayList<>();
        for (LockedBlock block : main.lockedBlocks){
            lockedBlocks.add(block.save());
        }

        File factionFile = new File(FILE_PATH + LOCKED_BLOCKS_FILE_NAME);
        writeOutFiles(factionFile, lockedBlocks);

        System.out.println("Locked blocks saved.");
    }

    private void writeOutFiles(File file, List<Map<String, String>> saveData) {
        try {
            if (file.createNewFile()) {
                System.out.println("Creating save file.");
            } else {
                System.out.println("Save file already exists, overwriting.");
            }

            FileWriter saveWriter = new FileWriter(file);
            saveWriter.write(gson.toJson(saveData));

        } catch(IOException e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

//    public void saveClaimedChunkFilenames() {
//        try {
//            File saveFolder = new File("./plugins/MedievalFactions/claimedchunks/");
//            if (!saveFolder.exists()) {
//                saveFolder.mkdir();
//            }
//            File saveFile = new File("./plugins/MedievalFactions/claimedchunks/" + "claimedchunks.txt");
//            if (saveFile.createNewFile()) {
//                System.out.println("Save file for claimed chunk filenames created.");
//            } else {
//                System.out.println("Save file for claimed chunk filenames already exists. Overwriting.");
//            }
//
//            FileWriter saveWriter = new FileWriter(saveFile);
//
//            // actual saving takes place here
//            for (ClaimedChunk chunk : main.claimedChunks) {
//                double[] coords = chunk.getCoordinates();
//
//                saveWriter.write((int)coords[0] + "_" + (int)coords[1] + ".txt" + "\n");
//            }
//
//            saveWriter.close();
//
//        } catch (IOException e) {
//            System.out.println("An error occurred while saving claimed chunk filenames.");
//        }
//    }
//
//    public void saveClaimedChunks() {
//        System.out.println("Saving claimed chunks...");
//        for (ClaimedChunk chunk : main.claimedChunks) {
//            chunk.save();
//        }
//        System.out.println("Claimed chunks saved.");
//    }
//
//    public void savePlayerPowerRecordFilenames() {
//        try {
//            File saveFolder = new File("./plugins/MedievalFactions/player-power-records/");
//            if (!saveFolder.exists()) {
//                saveFolder.mkdir();
//            }
//            File saveFile = new File("./plugins/MedievalFactions/player-power-records/" + "playerpowerrecords.txt");
//            if (saveFile.createNewFile()) {
//                System.out.println("Save file for player power record filenames created.");
//            } else {
//                System.out.println("Save file for player power record filenames already exists. Overwriting.");
//            }
//
//            FileWriter saveWriter = new FileWriter(saveFile);
//
//            // actual saving takes place here
//            for (PlayerPowerRecord record : main.playerPowerRecords) {
//                saveWriter.write(record.getPlayerName() + ".txt" + "\n");
//            }
//
//            saveWriter.close();
//
//        } catch (IOException e) {
//            System.out.println("An error occurred while saving player power record filenames.");
//        }
//    }
//
//    public void savePlayerPowerRecords() {
//        System.out.println("Saving player power records...");
//        for (PlayerPowerRecord record: main.playerPowerRecords) {
//            record.save();
//        }
//        System.out.println("Player power records saved.");
//    }
//
//    public void saveLockedBlockFilenames() {
//        try {
//            File saveFolder = new File("./plugins/MedievalFactions/lockedblocks/");
//            if (!saveFolder.exists()) {
//                saveFolder.mkdir();
//            }
//            File saveFile = new File("./plugins/MedievalFactions/lockedblocks/" + "lockedblocks.txt");
//            if (saveFile.createNewFile()) {
//                System.out.println("Save file for locked block filenames created.");
//            } else {
//                System.out.println("Save file for locked block filenames already exists. Overwriting.");
//            }
//
//            FileWriter saveWriter = new FileWriter(saveFile);
//
//            // actual saving takes place here
//            for (LockedBlock block : main.lockedBlocks) {
//                saveWriter.write(block.getX() + "_" + block.getY() + "_" + block.getZ() + ".txt" + "\n");
//            }
//
//            saveWriter.close();
//
//        } catch (IOException e) {
//            System.out.println("An error occurred while saving locked block filenames.");
//        }
//    }
//
//    public void saveLockedBlocks() {
//        for (LockedBlock block : main.lockedBlocks) {
//            block.save();
//        }
//    }

    public void load() {
        if (legacyFilesExists()){
            legacyLoadFactions();
            legacyLoadClaimedChunks();
            legacyLoadPlayerPowerRecords();
            legacyLoadLockedBlocks();
            deleteLegacyFiles();
        } else {
            loadFactions();
            loadClaimedChunks();
            loadPlayerPowerRecords();
            loadLockedBlocks();
        }
    }

    private boolean legacyFilesExists() {
        return  new File(FILE_PATH + LEGACY_FACTIONS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_CHUNKS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_LOCKED_BLOCKS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_PLAYERPOWER_FILE_NAME).exists();
    }

    private void deleteLegacyFiles() {
        boolean factionFile = new File(FILE_PATH + LEGACY_FACTIONS_FILE_NAME).delete();
        boolean chunkFile = new File(FILE_PATH + LEGACY_CHUNKS_FILE_NAME).delete();
        boolean lockedBlockFile = new File(FILE_PATH + LEGACY_LOCKED_BLOCKS_FILE_NAME).delete();
        boolean playerPowerFile = new File(FILE_PATH + LEGACY_PLAYERPOWER_FILE_NAME).delete();
        if (!factionFile || !chunkFile || !lockedBlockFile || !playerPowerFile){
            System.out.println("One of the legacy files failed to be deleted, please remove it.");
        }
    }

    private void loadFactions() {
        // Clean arraylist before loading
        main.factions.clear();
    }

    private void loadClaimedChunks() {
        main.claimedChunks.clear();
    }

    private void loadPlayerPowerRecords() {
        main.playerPowerRecords.clear();
    }

    private void loadLockedBlocks() {
        main.lockedBlocks.clear();
    }

    public void legacyLoadFactions() {
        try {
            System.out.println("Attempting to load factions...");
            File loadFile = new File("./plugins/MedievalFactions/" + "faction-names.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                Faction temp = new Faction(nextName, main.getConfig().getInt("maxPowerLevel")); // uses server constructor, only temporary
                temp.legacyLoad(nextName + ".txt"); // provides owner field among other things

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

    public void legacyLoadClaimedChunks() {
        System.out.println("Loading claimed chunks...");

        try {
            System.out.println("Attempting to load claimed chunks...");
            File loadFile = new File("./plugins/MedievalFactions/claimedchunks/" + "claimedchunks.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                ClaimedChunk temp = new ClaimedChunk(); // uses no-parameter constructor since load provides chunk
                temp.legacyLoad(nextName); // provides owner field among other things

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

    public void legacyLoadPlayerPowerRecords() {
        System.out.println("Loading player power records...");

        try {
            System.out.println("Attempting to load player power record filenames...");
            File loadFile = new File("./plugins/MedievalFactions/player-power-records/" + "playerpowerrecords.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                PlayerPowerRecord temp = new PlayerPowerRecord(main.getConfig().getInt("maxPowerLevel")); // uses no-parameter constructor since load provides name
                temp.legacyLoad(nextName); // provides power field among other things

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

    public void legacyLoadLockedBlocks() {
        System.out.println("Loading locked blocks...");

        try {
            System.out.println("Attempting to load locked blocks...");
            File loadFile = new File("./plugins/MedievalFactions/lockedblocks/" + "lockedblocks.txt");
            Scanner loadReader = new Scanner(loadFile);

            // actual loading
            while (loadReader.hasNextLine()) {
                String nextName = loadReader.nextLine();
                LockedBlock temp = new LockedBlock(); // uses no-parameter constructor since load provides chunk
                temp.legacyLoad(nextName);

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
