package dansplugins.factionsystem;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

public class ConfigManager {

    private static ConfigManager instance;

    private ConfigManager() {

    }

    public static ConfigManager getInstance() {
        if (instance == null) {
            instance = new ConfigManager();
        }
        return instance;
    }

    public void handleVersionMismatch() {
        // set version
        if (!MedievalFactions.getInstance().getConfig().isString("version")) {
            System.out.println("Version not set! Setting version to " + MedievalFactions.getInstance().getVersion());
            MedievalFactions.getInstance().getConfig().addDefault("version", MedievalFactions.getInstance().getVersion());
        }
        else {
            System.out.println("Version set but mismatched! Setting version to " + MedievalFactions.getInstance().getVersion());
            MedievalFactions.getInstance().getConfig().set("version", MedievalFactions.getInstance().getVersion());
        }

        // add defaults if they don't exist
        if (!MedievalFactions.getInstance().getConfig().isInt("initialMaxPowerLevel")) {
            System.out.println("Max power level not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("initialMaxPowerLevel", 20);
        }
        if (!MedievalFactions.getInstance().getConfig().isInt("initialPowerLevel")) {
            System.out.println("Initial power level not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("initialPowerLevel", 5);
        }
        if (!MedievalFactions.getInstance().getConfig().isBoolean("mobsSpawnInFactionTerritory")) {
            System.out.println("Mobs spawn in faction territory not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        }
        if (!MedievalFactions.getInstance().getConfig().isInt("powerIncreaseAmount")) {
            System.out.println("Hourly power increase amount not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("powerIncreaseAmount", 2);
        }
        if (!MedievalFactions.getInstance().getConfig().isBoolean("laddersPlaceableInEnemyFactionTerritory")) {
            System.out.println("Ladders placeable in enemy faction territory not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        }
        if (!MedievalFactions.getInstance().getConfig().isInt("minutesBeforeInitialPowerIncrease")) {
            System.out.println("minutesBeforeInitialPowerIncrease not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        }
        if (!MedievalFactions.getInstance().getConfig().isInt("minutesBetweenPowerIncreases")) {
            System.out.println("minutesBetweenPowerIncreases not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        }

        if (!MedievalFactions.getInstance().getConfig().isBoolean("warsRequiredForPVP")) {
            System.out.println("warsRequiredForPVP not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("warsRequiredForPVP", true);
        }

        if (!MedievalFactions.getInstance().getConfig().isDouble("factionOwnerMultiplier")) {
            System.out.println("factionOwnerMultiplier not set! Setting to default");
            MedievalFactions.getInstance().getConfig().addDefault("factionOwnerMultiplier", 2.0);
        }

        if (!MedievalFactions.getInstance().getConfig().isDouble("officerPerMemberCount")){
            System.out.println("officerPerMemberCount is not set! Setting to default");
            MedievalFactions.getInstance().getConfig().addDefault("officerPerMemberCount", 5);
        }

        if (!MedievalFactions.getInstance().getConfig().isDouble("factionOfficerMultiplier")){
            System.out.println("factionOfficerMultiplier is not set! Setting to default");
            MedievalFactions.getInstance().getConfig().addDefault("factionOfficerMultiplier", 1.5);
        }

        if (!MedievalFactions.getInstance().getConfig().isBoolean("powerDecreases")) {
        	System.out.println("powerDecreases is not set! Setting to default");
        	MedievalFactions.getInstance().getConfig().addDefault("powerDecreases", true);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("minutesBetweenPowerDecreases")) {
        	System.out.println("minutesBetweenPowerDecreases is not set! Setting to default");
        	MedievalFactions.getInstance().getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("minutesBeforePowerDecrease")) {
        	System.out.println("minutesBeforePowerDecrease is not set! Setting to default");
        	MedievalFactions.getInstance().getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("powerDecreaseAmount")) {
            System.out.println("powerDecreaseAmount not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("powerDecreaseAmount", 1);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("factionMaxNameLength")) {
        	System.out.println("factionMaxNameLength not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("factionMaxNameLength", 20);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("factionMaxNumberGates")) {
        	System.out.println("factionMaxNumberGates not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("factionMaxNumberGates", 5);
        }

        if (!MedievalFactions.getInstance().getConfig().isInt("factionMaxGateArea")) {
        	System.out.println("factionMaxGateArea not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("factionMaxGateArea", 64);
        }

        if (!MedievalFactions.getInstance().getConfig().isBoolean("surroundedChunksProtected")) {
            System.out.println("surroundedChunksProtected not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("surroundedChunksProtected", true);
        }

        if (!MedievalFactions.getInstance().getConfig().isBoolean("zeroPowerFactionsGetDisbanded")) {
            System.out.println("zeroPowerFactionsGetDisbanded not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
        }

        if (!MedievalFactions.getInstance().getConfig().isBoolean("vassalContributionPercentageMultiplier")) {
            System.out.println("vassalContributionPercentageMultiplier not set! Setting to default!");
            MedievalFactions.getInstance().getConfig().addDefault("vassalContributionPercentageMultiplier", 0.10);
        }

        deleteOldConfigOptionsIfPresent();

        MedievalFactions.getInstance().getConfig().options().copyDefaults(true);
        MedievalFactions.getInstance().saveConfig();
    }

    private void deleteOldConfigOptionsIfPresent() {

        if (MedievalFactions.getInstance().getConfig().isInt("officerLimit")) {
            MedievalFactions.getInstance().getConfig().set("officerLimit", null);
        }

        if (MedievalFactions.getInstance().getConfig().isInt("hourlyPowerIncreaseAmount")) {
            MedievalFactions.getInstance().getConfig().set("hourlyPowerIncreaseAmount", null);
        }

        if (MedievalFactions.getInstance().getConfig().isInt("maxPowerLevel")) {
            MedievalFactions.getInstance().getConfig().set("maxPowerLevel", null);
        }

    }

    public static void setConfigOption(String option, String value, Player player) {

        if (MedievalFactions.getInstance().getConfig().isSet(option)) {

            if (option.equalsIgnoreCase("version")) {
                player.sendMessage(ChatColor.RED + "Can't set version!");
                return;
            }
            else if (option.equalsIgnoreCase("initialMaxPowerLevel") || option.equalsIgnoreCase("initialPowerLevel")
                    || option.equalsIgnoreCase("powerIncreaseAmount")
                    || option.equalsIgnoreCase("minutesBeforeInitialPowerIncrease")
                    || option.equalsIgnoreCase("minutesBetweenPowerIncreases")
                    || option.equalsIgnoreCase("officerLimit")
                    || option.equalsIgnoreCase("officerPerMemberCount")
                    || option.equalsIgnoreCase("minutesBetweenPowerDecreases")
                    || option.equalsIgnoreCase("minutesBeforePowerDecrease")
                    || option.equalsIgnoreCase("powerDecreaseAmount")
                    || option.equalsIgnoreCase("factionMaxNameLength")
	                || option.equalsIgnoreCase("factionMaxNumberGates")
	                || option.equalsIgnoreCase("factionMaxGateArea"))
            {
                MedievalFactions.getInstance().getConfig().set(option, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + "Integer set!");
            }
            else if (option.equalsIgnoreCase("mobsSpawnInFactionTerritory")
                    || option.equalsIgnoreCase("laddersPlaceableInEnemyFactionTerritory")
                    || option.equalsIgnoreCase("warsRequiredForPVP")
                    || option.equalsIgnoreCase("powerDecreases")
                    || option.equalsIgnoreCase("surroundedChunksProtected")
                    || option.equalsIgnoreCase("zeroPowerFactionsGetDisbanded")) {
                MedievalFactions.getInstance().getConfig().set(option, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + "Boolean set!");
            }
            else if (option.equalsIgnoreCase("factionOwnerMultiplier")
                    || option.equalsIgnoreCase("factionOfficerMultiplier")
                    || option.equalsIgnoreCase("vassalContributionPercentageMultiplier")){
                MedievalFactions.getInstance().getConfig().set(option, Double.parseDouble(value));
                player.sendMessage(ChatColor.GREEN + "Double set!");
            }
            else {
                MedievalFactions.getInstance().getConfig().set(option, value);
                player.sendMessage(ChatColor.GREEN + "String set!");
            }

            // save
            MedievalFactions.getInstance().saveConfig();
        }
        else {
            player.sendMessage(ChatColor.RED + "'" + option + "' wasn't found.");
        }

    }

    public void saveConfigDefaults() {
        MedievalFactions.getInstance().getConfig().addDefault("version", MedievalFactions.getInstance().getVersion());
        MedievalFactions.getInstance().getConfig().addDefault("initialMaxPowerLevel", 20);
        MedievalFactions.getInstance().getConfig().addDefault("initialPowerLevel", 5);
        MedievalFactions.getInstance().getConfig().addDefault("powerIncreaseAmount", 2);
        MedievalFactions.getInstance().getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        MedievalFactions.getInstance().getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        MedievalFactions.getInstance().getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        MedievalFactions.getInstance().getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        MedievalFactions.getInstance().getConfig().addDefault("warsRequiredForPVP", true);
        MedievalFactions.getInstance().getConfig().addDefault("factionOwnerMultiplier", 2.0);
        MedievalFactions.getInstance().getConfig().addDefault("officerPerMemberCount", 5);
        MedievalFactions.getInstance().getConfig().addDefault("factionOfficerMultiplier", 1.5);
        MedievalFactions.getInstance().getConfig().addDefault("powerDecreases", true);
        MedievalFactions.getInstance().getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        MedievalFactions.getInstance().getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        MedievalFactions.getInstance().getConfig().addDefault("powerDecreaseAmount", 1);
        MedievalFactions.getInstance().getConfig().addDefault("factionMaxNameLength", 20);
        MedievalFactions.getInstance().getConfig().addDefault("factionMaxNumberGates", 5);
        MedievalFactions.getInstance().getConfig().addDefault("factionMaxGateArea", 64);
        MedievalFactions.getInstance().getConfig().addDefault("surroundedChunksProtected", true);
        MedievalFactions.getInstance().getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
        MedievalFactions.getInstance().getConfig().addDefault("vassalContributionPercentageMultiplier", 0.10);
        MedievalFactions.getInstance().getConfig().options().copyDefaults(true);
        MedievalFactions.getInstance().saveConfig();
    }

    public void sendPlayerConfigList(Player player) {
        player.sendMessage(ChatColor.AQUA + "version: " + MedievalFactions.getInstance().getConfig().getString("version")
                + ", initialMaxPowerLevel: " + MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel")
                + ", initialPowerLevel: " +  MedievalFactions.getInstance().getConfig().getInt("initialPowerLevel")
                + ", powerIncreaseAmount: " + MedievalFactions.getInstance().getConfig().getInt("powerIncreaseAmount")
                + ", mobsSpawnInFactionTerritory: " + MedievalFactions.getInstance().getConfig().getBoolean("mobsSpawnInFactionTerritory")
                + ", laddersPlaceableInEnemyFactionTerritory: " + MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")
                + ", minutesBeforeInitialPowerIncrease: " + MedievalFactions.getInstance().getConfig().getInt("minutesBeforeInitialPowerIncrease")
                + ", minutesBetweenPowerIncreases: " + MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerIncreases")
                + ", warsRequiredForPVP: " + MedievalFactions.getInstance().getConfig().getBoolean("warsRequiredForPVP")
                + ", factionOwnerMultiplier: " + MedievalFactions.getInstance().getConfig().getDouble("factionOwnerMultiplier")
                + ", officerPerMemberCount: " + MedievalFactions.getInstance().getConfig().getInt("officerPerMemberCount")
                + ", factionOfficerMultiplier: " + MedievalFactions.getInstance().getConfig().getDouble("factionOfficerMultiplier")
                + ", powerDecreases: " + MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")
                + ", minutesBetweenPowerDecreases: " + MedievalFactions.getInstance().getConfig().getInt("minutesBetweenPowerDecreases")
                + ", minutesBeforePowerDecrease: " + MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease")
                + ", powerDecreaseAmount: " + MedievalFactions.getInstance().getConfig().getInt("powerDecreaseAmount")
                + ", factionMaxNameLength: " + MedievalFactions.getInstance().getConfig().getInt("factionMaxNameLength")
		        + ", factionMaxNumberGates: " + MedievalFactions.getInstance().getConfig().getInt("factionMaxNumberGates")
		        + ", factionMaxGateArea: " + MedievalFactions.getInstance().getConfig().getInt("factionMaxGateArea")
                + ", surroundedChunksProtected: " + MedievalFactions.getInstance().getConfig().getBoolean("surroundedChunksProtected")
                + ", zeroPowerFactionsGetDisbanded: " + MedievalFactions.getInstance().getConfig().getBoolean("zeroPowerFactionsGetDisbanded")
                + ", vassalContributionPercentageMultiplier: " + MedievalFactions.getInstance().getConfig().getDouble("vassalContributionPercentageMultiplier"));
    }

}
