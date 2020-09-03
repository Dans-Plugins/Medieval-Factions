package factionsystem.Subsystems;

import factionsystem.Main;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

public class ConfigSubsystem {

    Main main = null;

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
        if (!main.getConfig().isInt("initialMaxPowerLevel")) {
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
        if (!main.getConfig().isInt("powerIncreaseAmount")) {
            System.out.println("Hourly power increase amount not set! Setting to default!");
            main.getConfig().addDefault("powerIncreaseAmount", 2);
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

        if (!main.getConfig().isBoolean("powerDecreases")) {
        	System.out.println("powerDecreases is not set! Setting to default");
        	main.getConfig().addDefault("powerDecreases", true);
        }

        if (!main.getConfig().isInt("minutesBetweenPowerDecreases")) {
        	System.out.println("minutesBetweenPowerDecreases is not set! Setting to default");
        	main.getConfig().addDefault("minutesBetweenPowerDecreases", 60);
        }
        
        if (!main.getConfig().isInt("minutesBetweenPowerDecreases")) {
        	System.out.println("minutesBetweenPowerDecreases is not set! Setting to default");
        	main.getConfig().addDefault("minutesBetweenPowerDecreases", 60);
        }
        
        if (!main.getConfig().isInt("minutesBeforePowerDecrease")) {
        	System.out.println("minutesBeforePowerDecrease is not set! Setting to default");
        	main.getConfig().addDefault("minutesBeforePowerDecrease", 6);
        }
        
        
        
        deleteOldConfigOptionsIfPresent();

        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

    private void deleteOldConfigOptionsIfPresent() {

        if (main.getConfig().isInt("officerLimit")) {
            main.getConfig().set("officerLimit", null);
        }

        if (main.getConfig().isInt("hourlyPowerIncreaseAmount")) {
            main.getConfig().set("hourlyPowerIncreaseAmount", null);
        }

        if (main.getConfig().isInt("maxPowerLevel")) {
            main.getConfig().set("maxPowerLevel", null);
        }

    }

    public void saveConfigDefaults() {
        main.getConfig().addDefault("version", main.version);
        main.getConfig().addDefault("initialMaxPowerLevel", 20);
        main.getConfig().addDefault("initialPowerLevel", 5);
        main.getConfig().addDefault("powerIncreaseAmount", 2);
        main.getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        main.getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        main.getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        main.getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        main.getConfig().addDefault("warsRequiredForPVP", true);
        main.getConfig().addDefault("factionOwnerMultiplier", 2.0);
        main.getConfig().addDefault("officerPerMemberCount", 5);
        main.getConfig().addDefault("factionOfficerMultiplier", 1.5);
        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

}
