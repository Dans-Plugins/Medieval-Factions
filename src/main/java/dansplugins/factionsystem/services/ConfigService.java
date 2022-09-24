/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;

import java.util.ArrayList;
import java.util.Collections;

/**
 * @author Daniel McCoy Stephenson
 */
public class ConfigService {
    private final MedievalFactions medievalFactions;
    private final LocaleService localeService;

    private boolean altered = false;

    public ConfigService(MedievalFactions medievalFactions) {
        this.medievalFactions = medievalFactions;
        localeService = new LocaleService(medievalFactions, this);
    }

    public void handleVersionMismatch() {
        if (!getConfig().isString("version")) {
            getConfig().set("version", medievalFactions.getVersion());
        } else {
            getConfig().set("version", medievalFactions.getVersion());
        }

        // add defaults if they don't exist
        if (!getConfig().isInt("initialMaxPowerLevel")) {
            getConfig().set("initialMaxPowerLevel", 20);
        }
        if (!getConfig().isInt("initialPowerLevel")) {
            getConfig().set("initialPowerLevel", 5);
        }
        if (!getConfig().isBoolean("mobsSpawnInFactionTerritory")) {
            getConfig().set("mobsSpawnInFactionTerritory", false);
        }
        if (!getConfig().isInt("powerIncreaseAmount")) {
            getConfig().set("powerIncreaseAmount", 2);
        }
        if (!getConfig().isBoolean("laddersPlaceableInEnemyFactionTerritory")) {
            getConfig().set("laddersPlaceableInEnemyFactionTerritory", true);
        }
        if (!getConfig().isInt("minutesBeforeInitialPowerIncrease")) {
            getConfig().set("minutesBeforeInitialPowerIncrease", 30);
        }
        if (!getConfig().isInt("minutesBetweenPowerIncreases")) {
            getConfig().set("minutesBetweenPowerIncreases", 60);
        }
        if (!getConfig().isBoolean("warsRequiredForPVP")) {
            getConfig().set("warsRequiredForPVP", true);
        }
        if (!getConfig().isDouble("factionOwnerMultiplier")) {
            getConfig().set("factionOwnerMultiplier", 2.0);
        }
        if (!getConfig().isDouble("officerPerMemberCount")) {
            getConfig().set("officerPerMemberCount", 5);
        }
        if (!getConfig().isDouble("factionOfficerMultiplier")) {
            getConfig().set("factionOfficerMultiplier", 1.5);
        }
        if (!getConfig().isBoolean("powerDecreases")) {
            getConfig().set("powerDecreases", true);
        }
        if (!getConfig().isInt("minutesBetweenPowerDecreases")) {
            getConfig().set("minutesBetweenPowerDecreases", 1440);
        }
        if (!getConfig().isInt("minutesBeforePowerDecrease")) {
            getConfig().set("minutesBeforePowerDecrease", 20160);
        }
        if (!getConfig().isInt("powerDecreaseAmount")) {
            getConfig().set("powerDecreaseAmount", 1);
        }
        if (!getConfig().isInt("factionMaxNameLength")) {
            getConfig().set("factionMaxNameLength", 20);
        }
        if (!getConfig().isInt("factionMaxNumberGates")) {
            getConfig().set("factionMaxNumberGates", 5);
        }
        if (!getConfig().isInt("factionMaxGateArea")) {
            getConfig().set("factionMaxGateArea", 64);
        }
        if (!getConfig().isBoolean("surroundedChunksProtected")) {
            getConfig().set("surroundedChunksProtected", true);
        }
        if (!getConfig().isBoolean("zeroPowerFactionsGetDisbanded")) {
            getConfig().set("zeroPowerFactionsGetDisbanded", false);
        }
        if (!getConfig().isDouble("vassalContributionPercentageMultiplier")) {
            getConfig().set("vassalContributionPercentageMultiplier", 0.75);
        }
        if (!getConfig().isBoolean("nonMembersCanInteractWithDoors")) {
            getConfig().set("nonMembersCanInteractWithDoors", false);
        }
        if (!getConfig().isBoolean("playersChatWithPrefixes")) {
            getConfig().set("playersChatWithPrefixes", true);
        }
        if (!getConfig().isInt("maxClaimRadius")) {
            getConfig().set("maxClaimRadius", 3);
        }
        if (!getConfig().isString("languageid")) {
            getConfig().set("languageid", "en-us");
        }
        if (!getConfig().isBoolean("chatSharedInVassalageTrees")) {
            getConfig().set("chatSharedInVassalageTrees", true);
        }
        if (!getConfig().isBoolean("allowAllyInteraction")) {
            getConfig().set("allowAllyInteraction", false);
        }
        if (!getConfig().isBoolean("allowVassalageTreeInteraction")) {
            getConfig().set("allowVassalageTreeInteraction", false);
        }
        if (!getConfig().isString("factionChatColor")) {
            getConfig().set("factionChatColor", "gold");
        }
        if (!getConfig().isBoolean("territoryAlertPopUp")) {
            getConfig().set("territoryAlertPopUp", true);
        }
        if (!getConfig().isBoolean("territoryIndicatorActionbar")) {
            getConfig().set("territoryIndicatorActionbar", true);
        }
        if (!getConfig().isString("territoryAlertColor")) {
            getConfig().set("territoryAlertColor", "white");
        }
        if (!getConfig().isBoolean("randomFactionAssignment")) {
            getConfig().set("randomFactionAssignment", false);
        }
        if (!getConfig().isBoolean("allowNeutrality")) {
            getConfig().set("allowNeutrality", false);
        }
        if (!getConfig().isBoolean("showPrefixesInFactionChat")) {
            getConfig().set("showPrefixesInFactionChat", false);
        }
        if (!getConfig().isBoolean("debugMode")) {
            getConfig().set("debugMode", false);
        }
        if (!getConfig().isBoolean("factionProtectionsEnabled")) {
            getConfig().set("factionProtectionsEnabled", true);
        }
        if (!getConfig().isBoolean("limitLand")) {
            getConfig().set("limitLand", true);
        }
        if (!getConfig().isBoolean("factionsCanSetPrefixColors")) {
            getConfig().set("factionsCanSetPrefixColors", true);
        }
        if (!getConfig().isBoolean("playersLosePowerOnDeath")) {
            getConfig().set("playersLosePowerOnDeath", true);
        }
        if (!getConfig().isBoolean("bonusPowerEnabled")) {
            getConfig().set("bonusPowerEnabled", true);
        }
        if (!getConfig().isBoolean("useNewLanguageFile")) {
            getConfig().set("useNewLanguageFile", true);
        }
        if (!getConfig().isDouble("powerLostOnDeath")) {
            getConfig().set("powerLostOnDeath", 1.0);
        }
        if (!getConfig().isDouble("powerGainedOnKill")) {
            getConfig().set("powerGainedOnKill", 1.0);
        }
        if (!getConfig().isInt("teleportDelay")) {
            getConfig().set("teleportDelay", 3);
        }
        if (!getConfig().isString("factionless")) {
            getConfig().set("factionless", "FactionLess");
        }
        if (!getConfig().isSet("secondsBeforeInitialAutosave")) {
            getConfig().set("secondsBeforeInitialAutosave", 60 * 60);
        }
        if (!getConfig().isSet("secondsBetweenAutosaves")) {
            getConfig().set("secondsBetweenAutosaves", 60 * 60);
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
                sender.sendMessage(ChatColor.RED + localeService.get("CannotSetVersion"));
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
                    || option.equalsIgnoreCase("teleportDelay")
                    || option.equalsIgnoreCase("secondsBeforeInitialAutosave")
                    || option.equalsIgnoreCase("secondsBetweenAutosaves")) {
                getConfig().set(option, Integer.parseInt(value));
                sender.sendMessage(ChatColor.GREEN + localeService.get("IntegerSet"));
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
                sender.sendMessage(ChatColor.GREEN + localeService.get("BooleanSet"));
            } else if (option.equalsIgnoreCase("factionOwnerMultiplier")
                    || option.equalsIgnoreCase("factionOfficerMultiplier")
                    || option.equalsIgnoreCase("vassalContributionPercentageMultiplier")
                    || option.equalsIgnoreCase("powerLostOnDeath")
                    || option.equalsIgnoreCase("powerGainedOnKill")) {
                getConfig().set(option, Double.parseDouble(value));
                sender.sendMessage(ChatColor.GREEN + localeService.get("DoubleSet"));
            } else {
                getConfig().set(option, value);
                sender.sendMessage(ChatColor.GREEN + localeService.get("StringSet"));

                if (option.equalsIgnoreCase("languageid")) {
                    localeService.reloadStrings();
                }
            }

            // save
            medievalFactions.saveConfig();
            altered = true;
        } else {
            sender.sendMessage(ChatColor.RED + String.format(localeService.get("WasntFound"), option));
        }

    }

    public void saveConfigDefaults() {
        getConfig().set("version", medievalFactions.getVersion());
        getConfig().set("initialMaxPowerLevel", 20);
        getConfig().set("initialPowerLevel", 5);
        getConfig().set("powerIncreaseAmount", 2);
        getConfig().set("mobsSpawnInFactionTerritory", false);
        getConfig().set("laddersPlaceableInEnemyFactionTerritory", true);
        getConfig().set("minutesBeforeInitialPowerIncrease", 30);
        getConfig().set("minutesBetweenPowerIncreases", 60);
        getConfig().set("warsRequiredForPVP", true);
        getConfig().set("factionOwnerMultiplier", 2.0);
        getConfig().set("officerPerMemberCount", 5);
        getConfig().set("factionOfficerMultiplier", 1.5);
        getConfig().set("powerDecreases", true);
        getConfig().set("minutesBetweenPowerDecreases", 1440);
        getConfig().set("minutesBeforePowerDecrease", 20160);
        getConfig().set("powerDecreaseAmount", 1);
        getConfig().set("factionMaxNameLength", 20);
        getConfig().set("factionMaxNumberGates", 5);
        getConfig().set("factionMaxGateArea", 64);
        getConfig().set("surroundedChunksProtected", true);
        getConfig().set("zeroPowerFactionsGetDisbanded", false);
        getConfig().set("vassalContributionPercentageMultiplier", 0.75);
        getConfig().set("nonMembersCanInteractWithDoors", false);
        getConfig().set("playersChatWithPrefixes", true);
        getConfig().set("maxClaimRadius", 3);
        getConfig().set("languageid", "en-us");
        getConfig().set("chatSharedInVassalageTrees", true);
        getConfig().set("allowAllyInteraction", false);
        getConfig().set("allowVassalageTreeInteraction", false);
        getConfig().set("factionChatColor", "gold");
        getConfig().set("territoryAlertPopUp", true);
        getConfig().set("territoryAlertColor", "white");
        getConfig().set("territoryIndicatorActionbar", true);
        getConfig().set("randomFactionAssignment", false);
        getConfig().set("allowNeutrality", false);
        getConfig().set("showPrefixesInFactionChat", false);
        getConfig().set("debugMode", false);
        getConfig().set("factionProtectionsEnabled", true);
        getConfig().set("limitLand", true);
        getConfig().set("factionsCanSetPrefixColors", true);
        getConfig().set("playersLosePowerOnDeath", true);
        getConfig().set("bonusPowerEnabled", true);
        getConfig().set("powerLostOnDeath", 1.0);
        getConfig().set("powerGainedOnKill", 1.0);
        getConfig().set("teleportDelay", 3);
        getConfig().set("factionless", "FactionLess");
        getConfig().set("useNewLanguageFile", true);
        getConfig().set("secondsBeforeInitialAutosave", 60);
        getConfig().set("secondsBetweenAutosaves", 60);
        getConfig().options().copyDefaults(true);
        medievalFactions.saveConfig();
    }

    public void sendPageOneOfConfigList(CommandSender sender) {
        sender.sendMessage(ChatColor.AQUA + localeService.get("ConfigListPageOne"));
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
        sender.sendMessage(ChatColor.AQUA + localeService.get("ConfigListPageTwo"));
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
                + ", teleportDelay: " + getInt("teleportDelay")
                + ", factionless: " + getString("factionless")
                + ", useNewLanguageFile: " + getBoolean("useNewLanguageFile")
                + ", secondsBeforeInitialAutosave: " + getInt("secondsBeforeInitialAutosave")
                + ", secondsBetweenAutosaves: " + getInt("secondsBetweenAutosaves"));
    }

    public ArrayList<String> getStringConfigOptions()
    {
        final ArrayList<String> configOptions = new ArrayList<>();
        Collections.addAll(configOptions,
                "initialMaxPowerLevel",
                "initialPowerLevel",
                "powerIncreaseAmount",
                "minutesBeforeInitialPowerIncrease",
                "minutesBetweenPowerIncreases",
                "officerLimit",
                "officerPerMemberCount",
                "minutesBetweenPowerDecreases",
                "minutesBeforePowerDecrease",
                "powerDecreaseAmount",
                "factionMaxNameLength",
                "factionMaxNumberGates",
                "factionMaxGateArea",
                "maxClaimRadius",
                "teleportDelay",
                "mobsSpawnInFactionTerritory",
                "laddersPlaceableInEnemyFactionTerritory",
                "warsRequiredForPVP",
                "powerDecreases",
                "surroundedChunksProtected",
                "zeroPowerFactionsGetDisbanded",
                "nonMembersCanInteractWithDoors",
                "playersChatWithPrefixes",
                "chatSharedInVassalageTrees",
                "allowAllyInteraction",
                "allowVassalageTreeInteraction",
                "territoryAlertPopUp",
                "territoryIndicatorActionbar",
                "randomFactionAssignment",
                "allowNeutrality",
                "showPrefixesInFactionChat",
                "debugMode",
                "factionProtectionsEnabled",
                "limitLand",
                "factionsCanSetPrefixColors",
                "playersLosePowerOnDeath",
                "bonusPowerEnabled",
                "factionOwnerMultiplier",
                "factionOfficerMultiplier",
                "vassalContributionPercentageMultiplier",
                "powerLostOnDeath",
                "powerGainedOnKill",
                "factionless",
                "useNewLanguageFile",
                "secondsBeforeInitialAutosave",
                "secondsBetweenAutosaves");
        return configOptions;
    }

    public boolean hasBeenAltered() {
        return altered;
    }

    public FileConfiguration getConfig() {
        return medievalFactions.getConfig();
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

    public LocaleService getLocaleService() {
        return localeService;
    }
}