package factionsystem.Subsystems;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import factionsystem.Main;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerPowerRecord;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

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

    private final static Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

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
        List<Map<String, String>> factions = new ArrayList<>();
        for (Faction faction : main.factions){
            factions.add(faction.save());
        }

        File file = new File(FILE_PATH + FACTIONS_FILE_NAME);
        writeOutFiles(file, factions);
    }

    private void saveClaimedChunks() {
        List<Map<String, String>> chunks = new ArrayList<>();
        for (ClaimedChunk chunk : main.claimedChunks){
            chunks.add(chunk.save());
        }

        File file = new File(FILE_PATH + CHUNKS_FILE_NAME);
        writeOutFiles(file, chunks);
    }

    private void savePlayerPowerRecords() {
        List<Map<String, String>> playerPowerRecords = new ArrayList<>();
        for (PlayerPowerRecord record : main.playerPowerRecords){
            playerPowerRecords.add(record.save());
        }

        File file = new File(FILE_PATH + PLAYERPOWER_FILE_NAME);
        writeOutFiles(file, playerPowerRecords);
    }

    private void saveLockedBlocks() {
        List<Map<String, String>> lockedBlocks = new ArrayList<>();
        for (LockedBlock block : main.lockedBlocks){
            lockedBlocks.add(block.save());
        }

        File file = new File(FILE_PATH + LOCKED_BLOCKS_FILE_NAME);
        writeOutFiles(file, lockedBlocks);
    }

    private void writeOutFiles(File file, List<Map<String, String>> saveData) {
        try {
            file.createNewFile();
            FileWriter saveWriter = new FileWriter(file);
            saveWriter.write(gson.toJson(saveData));
            saveWriter.close();
        } catch(IOException e) {
            System.out.println("ERROR: " + e.toString());
        }
    }

    public void load() {
        if (legacyFilesExists()){
            legacyLoadFactions();
            legacyLoadClaimedChunks();
            legacyLoadPlayerPowerRecords();
            legacyLoadLockedBlocks();
            deleteLegacyFiles();
            save();
        } else {
            System.out.println("Attempting to load Factions data...");
            loadFactions();
            loadClaimedChunks();
            loadPlayerPowerRecords();
            loadLockedBlocks();
            System.out.println("Faction data loaded successfully");
        }
    }

    private boolean legacyFilesExists() {
        return  new File(FILE_PATH + LEGACY_FACTIONS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_CHUNKS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_LOCKED_BLOCKS_FILE_NAME).exists() &&
                new File(FILE_PATH + LEGACY_PLAYERPOWER_FILE_NAME).exists();
    }

    private void deleteLegacyFiles() {
        if (!deleteDirectory(new File(FILE_PATH))){
            throw new RuntimeException("Legacy Files are not removed, and must be removable. If you are about to" +
                    " lose data, go back to before the save changes, v3.2 and below.");
        }
    }

    // Recursive file delete from https://www.baeldung.com/java-delete-directory
    boolean deleteDirectory(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteDirectory(file);
            }
        }
        return directoryToBeDeleted.delete();
    }

    private void loadFactions() {
        main.factions.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + FACTIONS_FILE_NAME);

        for (Map<String, String> factionData : data){
            Faction newFaction = new Faction(factionData);
            main.factions.add(newFaction);
        }
    }

    private void loadClaimedChunks() {
        main.claimedChunks.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + CHUNKS_FILE_NAME);

        for (Map<String, String> chunkData : data){
            ClaimedChunk chunk = new ClaimedChunk(chunkData);
            main.claimedChunks.add(chunk);
        }
    }

    private void loadPlayerPowerRecords() {
        main.playerPowerRecords.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + PLAYERPOWER_FILE_NAME);

        for (Map<String, String> powerRecord : data){
            PlayerPowerRecord player = new PlayerPowerRecord(powerRecord);
            main.playerPowerRecords.add(player);
        }
    }

    private void loadLockedBlocks() {
        main.lockedBlocks.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + LOCKED_BLOCKS_FILE_NAME);

        for (Map<String, String> lockedBlockData : data){
            LockedBlock lockedBlock = new LockedBlock(lockedBlockData);
            main.lockedBlocks.add(lockedBlock);
        }
    }

    private ArrayList<HashMap<String, String>> loadDataFromFilename(String filename) {
        try{
            Gson gson = new Gson();
            JsonReader reader = new JsonReader(new FileReader(filename));
            return gson.fromJson(reader, LIST_MAP_TYPE);
        } catch (FileNotFoundException e) {
            // Fail silently because this can actually happen in normal use
        }
        return new ArrayList<>();
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
                    if (main.playerPowerRecords.get(i).getPlayerUUID() == temp.getPlayerUUID()) {
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
