/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.utils.Locale;

/**
 * @author Daniel McCoy Stephenson
 */
public class ConfigService {
    private boolean altered = false;

    public void handleVersionMismatch() {
        if (!getConfig().isString("version")) {
            getConfig().addDefault("version", medievalFactions.getVersion());
        } else {
            getConfig().set("version", medievalFactions.getVersion());
        }

        // add defaults if they don't exist
        if (!getConfig().isInt("initialMaxPowerLevel")) {
            getConfig().addDefault("initialMaxPowerLevel", 20);
        }
        if (!getConfig().isInt("initialPowerLevel")) {
            getConfig().addDefault("initialPowerLevel", 5);
        }
        if (!getConfig().isBoolean("mobsSpawnInFactionTerritory")) {
            getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        }
        if (!getConfig().isInt("powerIncreaseAmount")) {
            getConfig().addDefault("powerIncreaseAmount", 2);
        }
        if (!getConfig().isBoolean("laddersPlaceableInEnemyFactionTerritory")) {
            getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        }
        if (!getConfig().isInt("minutesBeforeInitialPowerIncrease")) {
            getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        }
        if (!getConfig().isInt("minutesBetweenPowerIncreases")) {
            getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        }
        if (!getConfig().isBoolean("warsRequiredForPVP")) {
            getConfig().addDefault("warsRequiredForPVP", true);
        }
        if (!getConfig().isDouble("factionOwnerMultiplier")) {
            getConfig().addDefault("factionOwnerMultiplier", 2.0);
        }
        if (!getConfig().isDouble("officerPerMemberCount")) {
            getConfig().addDefault("officerPerMemberCount", 5);
        }
        if (!getConfig().isDouble("factionOfficerMultiplier")) {
            getConfig().addDefault("factionOfficerMultiplier", 1.5);
        }
        if (!getConfig().isBoolean("powerDecreases")) {
            getConfig().addDefault("powerDecreases", true);
        }
        if (!getConfig().isInt("minutesBetweenPowerDecreases")) {
            getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        }
        if (!getConfig().isInt("minutesBeforePowerDecrease")) {
            getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        }
        if (!getConfig().isInt("powerDecreaseAmount")) {
            getConfig().addDefault("powerDecreaseAmount", 1);
        }
        if (!getConfig().isInt("factionMaxNameLength")) {
            getConfig().addDefault("factionMaxNameLength", 20);
        }
        if (!getConfig().isInt("factionMaxNumberGates")) {
            getConfig().addDefault("factionMaxNumberGates", 5);
        }
        if (!getConfig().isInt("factionMaxGateArea")) {
            getConfig().addDefault("factionMaxGateArea", 64);
        }
        if (!getConfig().isBoolean("surroundedChunksProtected")) {
            getConfig().addDefault("surroundedChunksProtected", true);
        }
        if (!getConfig().isBoolean("zeroPowerFactionsGetDisbanded")) {
            getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
        }
        if (!getConfig().isDouble("vassalContributionPercentageMultiplier")) {
            getConfig().addDefault("vassalContributionPercentageMultiplier", 0.75);
        }
        if (!getConfig().isBoolean("nonMembersCanInteractWithDoors")) {
            getConfig().addDefault("nonMembersCanInteractWithDoors", false);
        }
        if (!getConfig().isBoolean("playersChatWithPrefixes")) {
            getConfig().addDefault("playersChatWithPrefixes", true);
        }
        if (!getConfig().isInt("maxClaimRadius")) {
            getConfig().addDefault("maxClaimRadius", 3);
        }
        if (!getConfig().isString("languageid")) {
            getConfig().addDefault("languageid", "en-us");
        }
        if (!getConfig().isBoolean("chatSharedInVassalageTrees")) {
            getConfig().addDefault("chatSharedInVassalageTrees", true);
        }
        if (!getConfig().isBoolean("allowAllyInteraction")) {
            getConfig().addDefault("allowAllyInteraction", false);
        }
        if (!getConfig().isBoolean("allowVassalageTreeInteraction")) {
            getConfig().addDefault("allowVassalageTreeInteraction", false);
        }
        if (!getConfig().isString("factionChatColor")) {
            getConfig().addDefault("factionChatColor", "gold");
        }
        if (!getConfig().isBoolean("territoryAlertPopUp")) {
            getConfig().addDefault("territoryAlertPopUp", true);
        }
        if (!getConfig().isBoolean("territoryIndicatorActionbar")) {
            getConfig().addDefault("territoryIndicatorActionbar", true);
        }
        if (!getConfig().isString("territoryAlertColor")) {
            getConfig().addDefault("territoryAlertColor", "white");
        }
        if (!getConfig().isBoolean("randomFactionAssignment")) {
            getConfig().addDefault("randomFactionAssignment", false);
        }
        if (!getConfig().isBoolean("allowNeutrality")) {
            getConfig().addDefault("allowNeutrality", false);
        }
        if (!getConfig().isBoolean("showPrefixesInFactionChat")) {
            getConfig().addDefault("showPrefixesInFactionChat", false);
        }
        if (!getConfig().isBoolean("debugMode")) {
            getConfig().addDefault("debugMode", false);
        }
        if (!getConfig().isBoolean("factionProtectionsEnabled")) {
            getConfig().addDefault("factionProtectionsEnabled", true);
        }
        if (!getConfig().isBoolean("limitLand")) {
            getConfig().addDefault("limitLand", true);
        }
        if (!getConfig().isBoolean("factionsCanSetPrefixColors")) {
            getConfig().addDefault("factionsCanSetPrefixColors", true);
        }
        if (!getConfig().isBoolean("playersLosePowerOnDeath")) {
            getConfig().addDefault("playersLosePowerOnDeath", true);
        }
        if (!getConfig().isBoolean("bonusPowerEnabled")) {
            getConfig().addDefault("bonusPowerEnabled", true);
        }
        if (!getConfig().isDouble("powerLostOnDeath")) {
            getConfig().addDefault("powerLostOnDeath", 1.0);
        }
        if (!getConfig().isDouble("powerGainedOnKill")) {
            getConfig().addDefault("powerGainedOnKill", 1.0);
        }
        if (!getConfig().isInt("teleportDelay")) {
            getConfig().addDefault("teleportDelay", 3);
        }

        deleteOldConfigOptionsIfPresent();

        getConfig().options().copyDefaults(true);
        medievalFactions.saveConfig();
    }

    private void deleteOldConfigOptionsIfPresent() {

        if (getConfig().isInt("officerLimit")) {
            getConfig().set("officerLimit", null);
        }

        if (getConfig().isInt("hourlyPowerIncreaseAmount")) {
            getConfig().set("hourlyPowerIncreaseAmount", null);
        }

        if (getConfig().isInt("maxPowerLevel")) {
            getConfig().set("maxPowerLevel", null);
        }

    }

    public void setConfigOption(String option, String value, CommandSender sender) {

        if (getConfig().isSet(option)) {

            if (option.equalsIgnoreCase("version")) {
                sender.sendMessage(ChatColor.RED + locale.get("CannotSetVersion"));
                return;
            } else if (option.equalsIgnoreCase("initialMaxPowerLevel") || option.equalsIgnoreCase("initialPowerLevel")
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
                    || option.equalsIgnoreCase("factionMaxGateArea")
                    || option.equalsIgnoreCase("maxClaimRadius")
                    || option.equalsIgnoreCase("teleportDelay")) {
                getConfig().set(option, Integer.parseInt(value));
                sender.sendMessage(ChatColor.GREEN + locale.get("IntegerSet"));
            } else if (option.equalsIgnoreCase("mobsSpawnInFactionTerritory")
                    || option.equalsIgnoreCase("laddersPlaceableInEnemyFactionTerritory")
                    || option.equalsIgnoreCase("warsRequiredForPVP")
                    || option.equalsIgnoreCase("powerDecreases")
                    || option.equalsIgnoreCase("surroundedChunksProtected")
                    || option.equalsIgnoreCase("zeroPowerFactionsGetDisbanded")
                    || option.equalsIgnoreCase("nonMembersCanInteractWithDoors")
                    || option.equalsIgnoreCase("playersChatWithPrefixes")
                    || option.equalsIgnoreCase("chatSharedInVassalageTrees")
                    || option.equalsIgnoreCase("allowAllyInteraction")
                    || option.equalsIgnoreCase("allowVassalageTreeInteraction")
                    || option.equalsIgnoreCase("territoryAlertPopUp")
                    || option.equalsIgnoreCase("territoryIndicatorActionbar")
                    || option.equalsIgnoreCase("randomFactionAssignment")
                    || option.equalsIgnoreCase("allowNeutrality")
                    || option.equalsIgnoreCase("showPrefixesInFactionChat")
                    || option.equalsIgnoreCase("debugMode")
                    || option.equalsIgnoreCase("factionProtectionsEnabled")
                    || option.equalsIgnoreCase("limitLand")
                    || option.equalsIgnoreCase("factionsCanSetPrefixColors")
                    || option.equalsIgnoreCase("playersLosePowerOnDeath")
                    || option.equalsIgnoreCase("bonusPowerEnabled")) {
                getConfig().set(option, Boolean.parseBoolean(value));
                sender.sendMessage(ChatColor.GREEN + locale.get("BooleanSet"));
            } else if (option.equalsIgnoreCase("factionOwnerMultiplier")
                    || option.equalsIgnoreCase("factionOfficerMultiplier")
                    || option.equalsIgnoreCase("vassalContributionPercentageMultiplier")
                    || option.equalsIgnoreCase("powerLostOnDeath")
                    || option.equalsIgnoreCase("powerGainedOnKill")) {
                getConfig().set(option, Double.parseDouble(value));
                sender.sendMessage(ChatColor.GREEN + locale.get("DoubleSet"));
            } else {
                getConfig().set(option, value);
                sender.sendMessage(ChatColor.GREEN + locale.get("StringSet"));

                if (option.equalsIgnoreCase("languageid")) {
                    locale.reloadStrings();
                }
            }

            // save
            medievalFactions.saveConfig();
            altered = true;
        } else {
            sender.sendMessage(ChatColor.RED + String.format(locale.get("WasntFound"), option));
        }

    }

    public void saveConfigDefaults() {
        getConfig().addDefault("version", medievalFactions.getVersion());
        getConfig().addDefault("initialMaxPowerLevel", 20);
        getConfig().addDefault("initialPowerLevel", 5);
        getConfig().addDefault("powerIncreaseAmount", 2);
        getConfig().addDefault("mobsSpawnInFactionTerritory", false);
        getConfig().addDefault("laddersPlaceableInEnemyFactionTerritory", true);
        getConfig().addDefault("minutesBeforeInitialPowerIncrease", 30);
        getConfig().addDefault("minutesBetweenPowerIncreases", 60);
        getConfig().addDefault("warsRequiredForPVP", true);
        getConfig().addDefault("factionOwnerMultiplier", 2.0);
        getConfig().addDefault("officerPerMemberCount", 5);
        getConfig().addDefault("factionOfficerMultiplier", 1.5);
        getConfig().addDefault("powerDecreases", true);
        getConfig().addDefault("minutesBetweenPowerDecreases", 1440);
        getConfig().addDefault("minutesBeforePowerDecrease", 20160);
        getConfig().addDefault("powerDecreaseAmount", 1);
        getConfig().addDefault("factionMaxNameLength", 20);
        getConfig().addDefault("factionMaxNumberGates", 5);
        getConfig().addDefault("factionMaxGateArea", 64);
        getConfig().addDefault("surroundedChunksProtected", true);
        getConfig().addDefault("zeroPowerFactionsGetDisbanded", false);
        getConfig().addDefault("vassalContributionPercentageMultiplier", 0.75);
        getConfig().addDefault("nonMembersCanInteractWithDoors", false);
        getConfig().addDefault("playersChatWithPrefixes", true);
        getConfig().addDefault("maxClaimRadius", 3);
        getConfig().addDefault("languageid", "en-us");
        getConfig().addDefault("chatSharedInVassalageTrees", true);
        getConfig().addDefault("allowAllyInteraction", false);
        getConfig().addDefault("allowVassalageTreeInteraction", false);
        getConfig().addDefault("factionChatColor", "gold");
        getConfig().addDefault("territoryAlertPopUp", true);
        getConfig().addDefault("territoryAlertColor", "white");
        getConfig().addDefault("territoryIndicatorActionbar", true);
        getConfig().addDefault("randomFactionAssignment", false);
        getConfig().addDefault("allowNeutrality", false);
        getConfig().addDefault("showPrefixesInFactionChat", false);
        getConfig().addDefault("debugMode", false);
        getConfig().addDefault("factionProtectionsEnabled", true);
        getConfig().addDefault("limitLand", true);
        getConfig().addDefault("factionsCanSetPrefixColors", true);
        getConfig().addDefault("playersLosePowerOnDeath", true);
        getConfig().addDefault("bonusPowerEnabled", true);
        getConfig().addDefault("powerLostOnDeath", 1.0);
        getConfig().addDefault("powerGainedOnKill", 1.0);
        getConfig().addDefault("teleportDelay", 3);
        getConfig().options().copyDefaults(true);
        medievalFactions.saveConfig();
    }

    public void sendPageOneOfConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + locale.get("ConfigListPageOne"));
        sender.sendMessage(ChatColor.AQUA + "version: " + getString("version")
                + ", languageid: " + getString("languageid")
                + ", debugMode: " + getBoolean("debugMode")
                + ", initialMaxPowerLevel: " + getInt("initialMaxPowerLevel")
                + ", initialPowerLevel: " + getInt("initialPowerLevel")
                + ", powerIncreaseAmount: " + getInt("powerIncreaseAmount")
                + ", mobsSpawnInFactionTerritory: " + getBoolean("mobsSpawnInFactionTerritory")
                + ", laddersPlaceableInEnemyFactionTerritory: " + getBoolean("laddersPlaceableInEnemyFactionTerritory")
                + ", minutesBeforeInitialPowerIncrease: " + getInt("minutesBeforeInitialPowerIncrease")
                + ", minutesBetweenPowerIncreases: " + getInt("minutesBetweenPowerIncreases")
                + ", warsRequiredForPVP: " + getBoolean("warsRequiredForPVP")
                + ", factionOwnerMultiplier: " + getDouble("factionOwnerMultiplier")
                + ", officerPerMemberCount: " + getInt("officerPerMemberCount")
                + ", factionOfficerMultiplier: " + getDouble("factionOfficerMultiplier")
                + ", powerDecreases: " + getBoolean("powerDecreases")
                + ", minutesBetweenPowerDecreases: " + getInt("minutesBetweenPowerDecreases")
                + ", minutesBeforePowerDecrease: " + getInt("minutesBeforePowerDecrease")
                + ", powerDecreaseAmount: " + getInt("powerDecreaseAmount")
                + ", factionMaxNameLength: " + getInt("factionMaxNameLength")
                + ", factionMaxNumberGates: " + getInt("factionMaxNumberGates"));
    }

    public void sendPageTwoOfConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + locale.get("ConfigListPageTwo"));
        sender.sendMessage(ChatColor.AQUA + "factionMaxGateArea: " + getInt("factionMaxGateArea")
                + ", surroundedChunksProtected: " + getBoolean("surroundedChunksProtected")
                + ", zeroPowerFactionsGetDisbanded: " + getBoolean("zeroPowerFactionsGetDisbanded")
                + ", vassalContributionPercentageMultiplier: " + getDouble("vassalContributionPercentageMultiplier")
                + ", nonMembersCanInteractWithDoors: " + getBoolean("nonMembersCanInteractWithDoors")
                + ", playersChatWithPrefixes: " + getBoolean("playersChatWithPrefixes")
                + ", maxClaimRadius: " + getInt("maxClaimRadius")
                + ", chatSharedInVassalageTrees: " + getBoolean("chatSharedInVassalageTrees")
                + ", allowAllyInteraction: " + getBoolean("allowAllyInteraction")
                + ", allowVassalageTreeInteraction: " + getBoolean("allowVassalageTreeInteraction")
                + ", factionChatColor: " + getString("factionChatColor")
                + ", territoryAlertPopUp: " + getBoolean("territoryAlertPopUp")
                + ", territoryAlertColor: " + getString("territoryAlertColor")
                + ", territoryIndicatorActionbar: " + getBoolean("territoryIndicatorActionbar")
                + ", randomFactionAssignment: " + getBoolean("randomFactionAssignment")
                + ", allowNeutrality: " + getBoolean("allowNeutrality")
                + ", showPrefixesInFactionChat: " + getBoolean("showPrefixesInFactionChat")
                + ", factionProtectionsEnabled: " + getBoolean("factionProtectionsEnabled")
                + ", limitLand: " + getBoolean("limitLand")
                + ", factionsCanSetPrefixColors: " + getBoolean("factionsCanSetPrefixColors")
                + ", playersLosePowerOnDeath: " + getBoolean("playersLosePowerOnDeath")
                + ", bonusPowerEnabled: " + getBoolean("bonusPowerEnabled")
                + ", powerLostOnDeath: " + getDouble("powerLostOnDeath")
                + ", powerGainedOnKill: " + getDouble("powerGainedOnKill")
                + ", teleportDelay: " + getInt("teleportDelay"));
    }

    public boolean hasBeenAltered() {
        return altered;
    }

    public FileConfiguration getConfig() {
        return configService;
    }

    public int getInt(String option) {
        return getConfig().getInt(option);
    }

    public boolean getBoolean(String option) {
        return getConfig().getBoolean(option);
    }

    public double getDouble(String option) {
        return getConfig().getDouble(option);
    }

    public String getString(String option) {
        return getConfig().getString(option);
    }
}