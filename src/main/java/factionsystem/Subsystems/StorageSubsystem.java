package factionsystem.Subsystems;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonReader;
import factionsystem.MedievalFactions;
import factionsystem.Objects.ClaimedChunk;
import factionsystem.Objects.Faction;
import factionsystem.Objects.LockedBlock;
import factionsystem.Objects.PlayerActivityRecord;
import factionsystem.Objects.PlayerPowerRecord;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

public class StorageSubsystem {

    private final static String FILE_PATH = "./plugins/MedievalFactions/";
    private final static String LEGACY_FACTIONS_FILE_NAME = "faction-names.txt";
    private final static String LEGACY_CHUNKS_FILE_NAME = "claimedchunks/claimedchunks.txt";
    private final static String LEGACY_PLAYERPOWER_FILE_NAME = "player-power-records/playerpowerrecords.txt";
    private final static String LEGACY_LOCKED_BLOCKS_FILE_NAME = "lockedblocks/lockedblocks.txt";
    private final static String FACTIONS_FILE_NAME = "factions.json";
    private final static String CHUNKS_FILE_NAME = "claimedchunks.json";
    private final static String PLAYERPOWER_FILE_NAME = "playerpowerrecords.json";
    private final static String PLAYERACTIVITY_FILE_NAME = "playeractivityrecords.json";
    private final static String LOCKED_BLOCKS_FILE_NAME = "lockedblocks.json";

    private final static Type LIST_MAP_TYPE = new TypeToken<ArrayList<HashMap<String, String>>>(){}.getType();

    private Gson gson = new GsonBuilder().setPrettyPrinting().create();;

    public void save() {
        saveFactions();
        saveClaimedChunks();
        savePlayerPowerRecords();
        savePlayerActivityRecords();
        saveLockedBlocks();
        MedievalFactions.getInstance().saveConfig();
    }

    private void saveFactions() {
        List<Map<String, String>> factions = new ArrayList<>();
        for (Faction faction : MedievalFactions.getInstance().factions){
            factions.add(faction.save());
        }

        File file = new File(FILE_PATH + FACTIONS_FILE_NAME);
        writeOutFiles(file, factions);
    }

    private void saveClaimedChunks() {
        List<Map<String, String>> chunks = new ArrayList<>();
        for (ClaimedChunk chunk : MedievalFactions.getInstance().claimedChunks){
            chunks.add(chunk.save());
        }

        File file = new File(FILE_PATH + CHUNKS_FILE_NAME);
        writeOutFiles(file, chunks);
    }

    private void savePlayerPowerRecords() {
        List<Map<String, String>> playerPowerRecords = new ArrayList<>();
        for (PlayerPowerRecord record : MedievalFactions.getInstance().playerPowerRecords){
            playerPowerRecords.add(record.save());
        }

        File file = new File(FILE_PATH + PLAYERPOWER_FILE_NAME);
        writeOutFiles(file, playerPowerRecords);
    }
    
    private void savePlayerActivityRecords()
    {
    	List<Map<String, String>> playerActivityRecords = new ArrayList<>();
    	for (PlayerActivityRecord record : MedievalFactions.getInstance().playerActivityRecords)
    	{
    		playerActivityRecords.add(record.save());
    		
    		File file = new File(FILE_PATH + PLAYERACTIVITY_FILE_NAME);
    		writeOutFiles(file, playerActivityRecords);
    	}
    }

    private void saveLockedBlocks() {
        List<Map<String, String>> lockedBlocks = new ArrayList<>();
        for (LockedBlock block : MedievalFactions.getInstance().lockedBlocks){
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
            loadPlayerActivityRecords();
            legacyLoadLockedBlocks();
            deleteLegacyFiles(new File(FILE_PATH));
            save();
        } else {
            System.out.println("Attempting to load Factions data...");
            loadFactions();
            loadClaimedChunks();
            loadPlayerPowerRecords();
            loadPlayerActivityRecords();
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

    // Recursive file delete from https://www.baeldung.com/java-delete-directory
    boolean deleteLegacyFiles(File directoryToBeDeleted) {
        File[] allContents = directoryToBeDeleted.listFiles();
        if (allContents != null) {
            for (File file : allContents) {
                deleteLegacyFiles(file);
            }
        }
        if (directoryToBeDeleted.getAbsolutePath().contains("config.yml")){
            return true;
        }
        return directoryToBeDeleted.delete();
    }

    private void loadFactions() {
        MedievalFactions.getInstance().factions.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + FACTIONS_FILE_NAME);

        for (Map<String, String> factionData : data){
            Faction newFaction = new Faction(factionData, MedievalFactions.getInstance());
            MedievalFactions.getInstance().factions.add(newFaction);
        }
    }

    private void loadClaimedChunks() {
        MedievalFactions.getInstance().claimedChunks.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + CHUNKS_FILE_NAME);

        for (Map<String, String> chunkData : data){
            ClaimedChunk chunk = new ClaimedChunk(chunkData);
            MedievalFactions.getInstance().claimedChunks.add(chunk);
        }
    }

    private void loadPlayerPowerRecords() {
        MedievalFactions.getInstance().playerPowerRecords.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + PLAYERPOWER_FILE_NAME);

        for (Map<String, String> powerRecord : data){
            PlayerPowerRecord player = new PlayerPowerRecord(powerRecord, MedievalFactions.getInstance());
            MedievalFactions.getInstance().playerPowerRecords.add(player);
        }
    }
    
    private void loadPlayerActivityRecords() {
        MedievalFactions.getInstance().playerActivityRecords.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + PLAYERACTIVITY_FILE_NAME);

        for (Map<String, String> powerRecord : data){
        	PlayerActivityRecord player = new PlayerActivityRecord(powerRecord, MedievalFactions.getInstance());
            MedievalFactions.getInstance().playerActivityRecords.add(player);
        }
    }

    private void loadLockedBlocks() {
        MedievalFactions.getInstance().lockedBlocks.clear();

        ArrayList<HashMap<String, String>> data = loadDataFromFilename(FILE_PATH + LOCKED_BLOCKS_FILE_NAME);

        for (Map<String, String> lockedBlockData : data){
            LockedBlock lockedBlock = new LockedBlock(lockedBlockData);
            MedievalFactions.getInstance().lockedBlocks.add(lockedBlock);
        }
    }

    private ArrayList<HashMap<String, String>> loadDataFromFilename(String filename) {
        try{
            Gson gson = new GsonBuilder().setPrettyPrinting().create();;
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
                Faction temp = new Faction(nextName, MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel"), MedievalFactions.getInstance()); // uses server constructor, only temporary
                temp.legacyLoad(nextName + ".txt"); // provides owner field among other things

                // existence check
                for (int i = 0; i < MedievalFactions.getInstance().factions.size(); i++) {
                    if (MedievalFactions.getInstance().factions.get(i).getName().equalsIgnoreCase(temp.getName())) {
                        MedievalFactions.getInstance().factions.remove(i);
                        break;
                    }
                }

                MedievalFactions.getInstance().factions.add(temp);

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
                for (int i = 0; i < MedievalFactions.getInstance().claimedChunks.size(); i++) {
                    if (MedievalFactions.getInstance().claimedChunks.get(i).getChunk().getX() == temp.getChunk().getX() &&
                            MedievalFactions.getInstance().claimedChunks.get(i).getChunk().getZ() == temp.getChunk().getZ()) {
                        MedievalFactions.getInstance().claimedChunks.remove(i);
                        break;
                    }
                }

                MedievalFactions.getInstance().claimedChunks.add(temp);

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
                PlayerPowerRecord temp = new PlayerPowerRecord(MedievalFactions.getInstance()); // uses no-parameter constructor since load provides name
                temp.legacyLoad(nextName); // provides power field among other things

                for (int i = 0; i < MedievalFactions.getInstance().playerPowerRecords.size(); i++) {
                    if (MedievalFactions.getInstance().playerPowerRecords.get(i).getPlayerUUID().equals(temp.getPlayerUUID())) {
                        MedievalFactions.getInstance().playerPowerRecords.remove(i);
                        break;
                    }
                }

                MedievalFactions.getInstance().playerPowerRecords.add(temp);
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
                for (int i = 0; i < MedievalFactions.getInstance().lockedBlocks.size(); i++) {
                    if (MedievalFactions.getInstance().lockedBlocks.get(i).getX() == temp.getX() && MedievalFactions.getInstance().lockedBlocks.get(i).getY() == temp.getY() && MedievalFactions.getInstance().lockedBlocks.get(i).getZ() == temp.getZ()) {
                        MedievalFactions.getInstance().lockedBlocks.remove(i);
                    }
                }

                MedievalFactions.getInstance().lockedBlocks.add(temp);

            }

            loadReader.close();
            System.out.println("Claimed chunks successfully loaded.");
        } catch (FileNotFoundException e) {
            System.out.println("There was a problem loading the claimed chunks!");
        }

        System.out.println("Claimed chunks loaded.");
    }

}
