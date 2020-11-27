package factionsystem.Subsystems;

import factionsystem.Main;

import java.io.*;
import java.util.Arrays;
import java.util.List;
import java.util.Scanner;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

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
            main.getConfig().addDefault("factionOfficerMultiplier", 1.5);
        }

        if (!main.getConfig().isBoolean("powerDecreases")) {
        	System.out.println("powerDecreases is not set! Setting to default");
        	main.getConfig().addDefault("powerDecreases", true);
        }

        if (!main.getConfig().isInt("minutesBetweenPowerDecreases")) {
        	System.out.println("minutesBetweenPowerDecreases is not set! Setting to default");
        	main.getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        }

        if (!main.getConfig().isInt("minutesBeforePowerDecrease")) {
        	System.out.println("minutesBeforePowerDecrease is not set! Setting to default");
        	main.getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        }
        
        if (!main.getConfig().isInt("powerDecreaseAmount")) {
            System.out.println("powerDecreaseAmount not set! Setting to default!");
            main.getConfig().addDefault("powerDecreaseAmount", 1);
        }
        
        if (!main.getConfig().isInt("factionMaxNameLength")) {
        	System.out.println("factionMaxNameLength not set! Setting to default!");
            main.getConfig().addDefault("factionMaxNameLength", 20);
        }

        if (!main.getConfig().isInt("factionMaxNumberGates")) {
        	System.out.println("factionMaxNumberGates not set! Setting to default!");
            main.getConfig().addDefault("factionMaxNumberGates", 5);
        }

        if (!main.getConfig().isInt("factionMaxGateArea")) {
        	System.out.println("factionMaxGateArea not set! Setting to default!");
            main.getConfig().addDefault("factionMaxGateArea", 64);
        }

        if (!main.getConfig().isBoolean("surroundedChunksProtected")) {
            System.out.println("surroundedChunksProtected not set! Setting to default!");
            main.getConfig().addDefault("surroundedChunksProtected", true);
        }

        if (!main.getConfig().isBoolean("zeroPowerFactionsGetDisbanded")) {
            System.out.println("zeroPowerFactionsGetDisbanded not set! Setting to default!");
            main.getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
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
    
    public static void setConfigOption(String option, String value, Player player, Main main) {

        if (main.getConfig().isSet(option)) {

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
                main.getConfig().set(option, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + "Integer set!");
            }
            else if (option.equalsIgnoreCase("mobsSpawnInFactionTerritory")
                    || option.equalsIgnoreCase("laddersPlaceableInEnemyFactionTerritory")
                    || option.equalsIgnoreCase("warsRequiredForPVP")
                    || option.equalsIgnoreCase("powerDecreases")
                    || option.equalsIgnoreCase("surroundedChunksProtected")
                    || option.equalsIgnoreCase("zeroPowerFactionsGetDisbanded")) {
                main.getConfig().set(option, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + "Boolean set!");
            }
            else if (option.equalsIgnoreCase("factionOwnerMultiplier")
                    || option.equalsIgnoreCase("factionOfficerMultiplier")){
                main.getConfig().set(option, Double.parseDouble(value));
                player.sendMessage(ChatColor.GREEN + "Double set!");
            }
            else {
                main.getConfig().set(option, value);
                player.sendMessage(ChatColor.GREEN + "String set!");
            }

            // save
            main.saveConfig();
        }
        else {
            player.sendMessage(ChatColor.RED + "'" + option + "' wasn't found.");
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
        main.getConfig().addDefault("powerDecreases", true);
        main.getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        main.getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        main.getConfig().addDefault("powerDecreaseAmount", 1);
        main.getConfig().addDefault("factionMaxNameLength", 20);
        main.getConfig().addDefault("factionMaxNumberGates", 5);
        main.getConfig().addDefault("factionMaxGateArea", 64);
        main.getConfig().addDefault("surroundedChunksProtected", true);
        main.getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
        main.getConfig().options().copyDefaults(true);
        main.saveConfig();
    }

    public void sendPlayerConfigList(Player player) {
        player.sendMessage(ChatColor.AQUA + "version: " + main.getConfig().getString("version")
                + ", initialMaxPowerLevel: " + main.getConfig().getInt("initialMaxPowerLevel")
                + ", initialPowerLevel: " +  main.getConfig().getInt("initialPowerLevel")
                + ", powerIncreaseAmount: " + main.getConfig().getInt("powerIncreaseAmount")
                + ", mobsSpawnInFactionTerritory: " + main.getConfig().getBoolean("mobsSpawnInFactionTerritory")
                + ", laddersPlaceableInEnemyFactionTerritory: " + main.getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")
                + ", minutesBeforeInitialPowerIncrease: " + main.getConfig().getInt("minutesBeforeInitialPowerIncrease")
                + ", minutesBetweenPowerIncreases: " + main.getConfig().getInt("minutesBetweenPowerIncreases")
                + ", warsRequiredForPVP: " + main.getConfig().getBoolean("warsRequiredForPVP")
                + ", factionOwnerMultiplier: " + main.getConfig().getDouble("factionOwnerMultiplier")
                + ", officerPerMemberCount: " + main.getConfig().getInt("officerPerMemberCount")
                + ", factionOfficerMultiplier: " + main.getConfig().getDouble("factionOfficerMultiplier")
                + ", powerDecreases: " + main.getConfig().getBoolean("powerDecreases")
                + ", minutesBetweenPowerDecreases: " + main.getConfig().getInt("minutesBetweenPowerDecreases")
                + ", minutesBeforePowerDecrease: " + main.getConfig().getInt("minutesBeforePowerDecrease")
                + ", powerDecreaseAmount: " + main.getConfig().getInt("powerDecreaseAmount")
                + ", factionMaxNameLength: " + main.getConfig().getInt("factionMaxNameLength")
		        + ", factionMaxNumberGates: " + main.getConfig().getInt("factionMaxNumberGates")
		        + ", factionMaxGateArea: " + main.getConfig().getInt("factionMaxGateArea")
                + ", surroundedChunksProtected: " + main.getConfig().getBoolean("surroundedChunksProtected")
                + ", zeroPowerFactionsGetDisbanded: " + main.getConfig().getBoolean("zeroPowerFactionsGetDisbanded"));
    }

}
