package dansplugins.factionsystem.objects;

import dansplugins.factionsystem.DynmapIntegrator;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.managers.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class FactionFlags {

    /*
        In order to add a new faction flag to this class, the following methods need to be altered:
        - initializeFlagNames()
        - initializeFlagValues()
        - loadMissingFlagsIfNecessary()
    */

    private final boolean debug = false;

    private ArrayList<String> flagNames = new ArrayList<>();
    private HashMap<String, Integer> integerValues = new HashMap<>();
    private HashMap<String, Boolean> booleanValues = new HashMap<>();
    private HashMap<String, Double> doubleValues = new HashMap<>();
    private HashMap<String, String> stringValues = new HashMap<>();

    public FactionFlags() {
        initializeFlagNames();
    }

    private void initializeFlagNames() { // this is called internally
        flagNames.add("mustBeOfficerToManageLand");
        flagNames.add("mustBeOfficerToInviteOthers");
        flagNames.add("alliesCanInteractWithLand");
        flagNames.add("vassalageTreeCanInteractWithLand");
        flagNames.add("neutral");
        flagNames.add("dynmapTerritoryColor");
        flagNames.add("territoryAlertColor");
        flagNames.add("prefixColor");
    }

    public void initializeFlagValues() {
        // this is called externally in Faction.java when a faction is created in-game
        booleanValues.put("mustBeOfficerToManageLand", true);
        booleanValues.put("mustBeOfficerToInviteOthers", true);
        booleanValues.put("alliesCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowAllyInteraction"));
        booleanValues.put("vassalageTreeCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowVassalageTreeInteraction"));
        booleanValues.put("neutral", false);
        stringValues.put("dynmapTerritoryColor", "#ff0000");
        stringValues.put("territoryAlertColor", MedievalFactions.getInstance().getConfig().getString("territoryAlertColor"));
        stringValues.put("prefixColor", "white");
    }

    public void loadMissingFlagsIfNecessary() {
        // this is called externally in Faction.java when a faction is loaded from save files
        if (!booleanValues.containsKey("mustBeOfficerToManageLand")) {
            booleanValues.put("mustBeOfficerToManageLand", true);
        }
        if (!booleanValues.containsKey("mustBeOfficerToInviteOthers")) {
            booleanValues.put("mustBeOfficerToInviteOthers", true);
        }
        if (!booleanValues.containsKey("alliesCanInteractWithLand")) {
            booleanValues.put("alliesCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowAllyInteraction"));
        }
        if (!booleanValues.containsKey("vassalageTreeCanInteractWithLand")) {
            booleanValues.put("vassalageTreeCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowVassalageTreeInteraction"));
        }
        if (!booleanValues.containsKey("neutral")) {
            booleanValues.put("neutral", false);
        }
        if (!stringValues.containsKey("dynmapTerritoryColor")) {
            stringValues.put("dynmapTerritoryColor", "#ff0000");
        }
        if (!stringValues.containsKey("territoryAlertColor")) {
            stringValues.put("territoryAlertColor", MedievalFactions.getInstance().getConfig().getString("territoryAlertColor"));
        }
        if (!stringValues.containsKey("prefixColor")) {
            stringValues.put("prefixColor", "white");
        }
    }

    public void sendFlagList(Player player) {
        player.sendMessage(ChatColor.AQUA + "" + getFlagsSeparatedByCommas());
    }


    public void setFlag(String flag, String value, Player player) {
        if (flag.equals("neutral") && !MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality")) {
            player.sendMessage(ChatColor.RED + "" + LocaleManager.getInstance().getText("NeutralityDisabled"));
            return;
        }

        if (flag.equals("prefixColor") && !MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes")) {
            player.sendMessage(ChatColor.RED + "" + LocaleManager.getInstance().getText("PrefixesDisabled"));
            return;
        }

        if (isFlag(flag)) {
            if (integerValues.containsKey(flag)) {
                integerValues.replace(flag, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("IntegerSet"));
            }
            else if (booleanValues.containsKey(flag)) {
                booleanValues.replace(flag, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("BooleanSet"));
            }
            else if (doubleValues.containsKey(flag)) {
                doubleValues.replace(flag, Double.parseDouble(value));
                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("DoubleSet"));
            }
            else if (stringValues.containsKey(flag)) {
                stringValues.replace(flag, value);
                player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("StringSet"));
            }

            if (flag.equals("dynmapTerritoryColor")) {
                DynmapIntegrator.getInstance().updateClaims(); // update dynmap to reflect color change
            }
        }
        else {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("WasntFound"), flag));
        }
    }

    public Object getFlag(String flag) {
        if (!isFlag(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was not found!", flag)); }
            return false;
        }

        if (integerValues.containsKey(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, integerValues.get(flag))); }
            return integerValues.get(flag);
        }
        else if (booleanValues.containsKey(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, booleanValues.get(flag))); }
            return booleanValues.get(flag);
        }
        else if (doubleValues.containsKey(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, doubleValues.get(flag))); }
            return doubleValues.get(flag);
        }
        else if (stringValues.containsKey(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, stringValues.get(flag))); }
            return stringValues.get(flag);
        }
        return null;
    }

    public HashMap<String, Integer> getIntegerValues() {
        return integerValues;
    }

    public void setIntegerValues(HashMap<String, Integer> values) {
        integerValues = values;
    }

    public HashMap<String, Boolean> getBooleanValues() {
        return booleanValues;
    }

    public void setBooleanValues(HashMap<String, Boolean> values) {
        booleanValues = values;
    }

    public HashMap<String, Double> getDoubleValues() {
        return doubleValues;
    }

    public void setDoubleValues(HashMap<String, Double> values) {
        doubleValues = values;
    }

    public HashMap<String, String> getStringValues() {
        return stringValues;
    }

    public void setStringValues(HashMap<String, String> values) {
        stringValues = values;
    }

    private boolean isFlag(String flag) {
        // this method will likely need to be used to sanitize user input
        return flagNames.contains(flag);
    }

    public int getNumFlags() {
        return booleanValues.size();
    }

    private String getFlagsSeparatedByCommas() {
        String toReturn = "";
        for (String flagName : flagNames) {

            if (flagName.equals("neutral") && !MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality")) {
                continue;
            }

            if (flagName.equals("prefixColor") && !MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes")) {
                continue;
            }

            if (!toReturn.equals("")) {
                toReturn += ", ";
            }
            if (integerValues.containsKey(flagName)) {
                toReturn += String.format("%s: %s", flagName, integerValues.get(flagName));
            }
            else if (booleanValues.containsKey(flagName)) {
                toReturn += String.format("%s: %s", flagName, booleanValues.get(flagName));
            }
            else if (doubleValues.containsKey(flagName)) {
                toReturn += String.format("%s: %s", flagName, doubleValues.get(flagName));
            }
            else if (stringValues.containsKey(flagName)) {
                toReturn += String.format("%s: %s", flagName, stringValues.get(flagName));
            }
        }
        return toReturn;
    }

}