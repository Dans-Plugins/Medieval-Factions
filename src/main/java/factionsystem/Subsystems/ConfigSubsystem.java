package factionsystem.Subsystems;

import factionsystem.Main;

import java.io.*;
import java.util.Arrays;
import java.util.List;

public class ConfigSubsystem {

    Main main = null;

    private final List<String> oldConfigOptions = Arrays.asList("officerLimit", "hourlyPowerIncreaseAmount", "maxPowerLevel");

    public ConfigSubsystem(Main plugin) {
        main = plugin;
    }

    public void handleVersionMismatch() {
        // set version
        if (!main.getConfig().isString("version")) {
            System.out.println("Version not set! Setting version to " + main.version);
            main.getConfig().addDefault("version", main.version);
        }
        else {
            System.out.println("Version set but mismatched! Setting version to " + main.version);
            main.getConfig().set("version", main.version);
        }

        // add defaults if they don't exist
        if (!main.getConfig().isInt("initialMaxPowerLevel")) { // TODO: change all references to maxPowerLevel
            System.out.println("Max power level not set! Setting to default!");
            main.getConfig().addDefault("initialMaxPowerLevel", 20);
        }
        if (!main.getConfig().isInt("initialPowerLevel")) {
            System.out.println("Initial power level not set! Setting to default!");
            main.getConfig().addDefault("initialPowerLevel", 5);
        }
        if (!main.getConfig().isBoolean("mobsSpawnInFactionTerritory")) {
            System.out.println("Mobs spawn in faction territory not set! Setting to default!");
            main.getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        }
        if (!main.getConfig().isInt("powerIncreaseAmount")) { // TODO: change all references to hourlyPowerIncreaseAmount
            System.out.println("Hourly power increase amount not set! Setting to default!");
            main.getConfig().addDefault("hourlyPowerIncreaseAmount", 2);
        }
        if (!main.getConfig().isBoolean("laddersPlaceableInEnemyFactionTerritory")) {
            System.out.println("Ladders placeable in enemy faction territory not set! Setting to default!");
            main.getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        }
        if (!main.getConfig().isInt("minutesBeforeInitialPowerIncrease")) {
            System.out.println("minutesBeforeInitialPowerIncrease not set! Setting to default!");
            main.getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        }
        if (!main.getConfig().isInt("minutesBetweenPowerIncreases")) {
            System.out.println("minutesBetweenPowerIncreases not set! Setting to default!");
            main.getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        }

        if (!main.getConfig().isBoolean("warsRequiredForPVP")) {
            System.out.println("warsRequiredForPVP not set! Setting to default!");
            main.getConfig().addDefault("warsRequiredForPVP", true);
        }

        if (!main.getConfig().isBoolean("officerLimit")) {
            System.out.println("officerLimit not set! Setting to default!");
            main.getConfig().addDefault("officerLimit", 0);
        }

        if (!main.getConfig().isDouble("factionOwnerMultiplier")) {
            System.out.println("factionOwnerMultiplier not set! Setting to default");
            main.getConfig().addDefault("factionOwnerMultiplier", 2.0);
        }

        if (!main.getConfig().isDouble("officerPerMemberCount")){
            System.out.println("officerPerMemberCount is not set! Setting to default");
            main.getConfig().addDefault("officerPerMemberCount", 5);
        }

        if (!main.getConfig().isDouble("factionOfficerMultiplier")){
            System.out.println("factionOfficerMultiplier is not set! Setting to default");
            main.getConfig().addDefault("factionOfficerMultiplier", 2.0);
        }

        deleteOldConfigOptionsIfPresent();

        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

    private void deleteOldConfigOptionsIfPresent() {
        for (String option : oldConfigOptions) {
            if (main.getConfig().isSet(option)) {
                deleteConfigOption(option);
            }
        }
    }

    // Credit: https://stackoverflow.com/questions/1377279/find-a-line-in-a-file-and-remove-it
    private void deleteConfigOption(String option) {
        try {
            // iterate through lines in config.yml
            File inputFile = new File("/plugins/MedievalFactions/config.yml");
            File tempFile = new File("/plugins/MedievalFactions/temp-config.yml");

            BufferedReader reader = new BufferedReader(new FileReader(inputFile));
            BufferedWriter writer = new BufferedWriter(new FileWriter(tempFile));

            String lineToRemove = option;
            String currentLine;

            while((currentLine = reader.readLine()) != null) {
                // trim newline when comparing with lineToRemove
                String trimmedLine = currentLine.trim();
                if(trimmedLine.contains(lineToRemove)) continue;
                writer.write(currentLine + System.getProperty("line.separator"));
            }
            writer.close();
            reader.close();
            tempFile.renameTo(inputFile);
        }
        catch(Exception e) {
            System.out.println("Something went wrong when deleting a config option.");
        }


    }

    public void saveConfigDefaults() {
        main.getConfig().addDefault("version", main.version);
        main.getConfig().addDefault("maxPowerLevel", 20);
        main.getConfig().addDefault("initialPowerLevel", 5);
        main.getConfig().addDefault("hourlyPowerIncreaseAmount", 2);
        main.getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        main.getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        main.getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        main.getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        main.getConfig().addDefault("warsRequiredForPVP", true);
        main.getConfig().addDefault("officerLimit", 0);
        main.getConfig().addDefault("factionOwnerMultiplier", 2.0);
        main.getConfig().addDefault("officerPerMemberCount", 5);
        main.getConfig().addDefault("factionOfficerMultiplier", 1.5);
        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

}
