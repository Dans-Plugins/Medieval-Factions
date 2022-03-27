/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.helper;

import java.awt.Color;
import java.util.ArrayList;
import java.util.HashMap;

import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.services.LocalConfigService;
import dansplugins.factionsystem.utils.ColorConversion;
import dansplugins.factionsystem.utils.Locale;
import dansplugins.factionsystem.utils.Logger;

/**
 * @author Daniel McCoy Stephenson
 * In order to add a new faction flag to this class, the following methods need to be altered:
 * - initializeFlagNames()
 * - initializeFlagValues()
 * - loadMissingFlagsIfNecessary()
 */
public class FactionFlags {
    private final ArrayList<String> flagNames = new ArrayList<>();
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
        flagNames.add("allowFriendlyFire");
        flagNames.add("fiefsEnabled");
        flagNames.add("officersCanMintCurrency");
        flagNames.add("acceptBonusPower");
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
        booleanValues.put("allowFriendlyFire", false);
        booleanValues.put("fiefsEnabled", true);
        booleanValues.put("officersCanMintCurrency", false);
        booleanValues.put("acceptBonusPower", true);
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
        if (!booleanValues.containsKey("allowFriendlyFire")) {
            booleanValues.put("allowFriendlyFire", false);
        }
        if (!booleanValues.containsKey("fiefsEnabled")) {
            booleanValues.put("fiefsEnabled", true);
        }
        if (!booleanValues.containsKey("officersCanMintCurrency")) {
            booleanValues.put("officersCanMintCurrency", false);
        }
        if (!booleanValues.containsKey("acceptBonusPower")) {
            booleanValues.put("acceptBonusPower", true);
        }
    }

    public void sendFlagList(Player player) {
        player.sendMessage(ChatColor.AQUA + "" + getFlagsSeparatedByCommas());
    }

    public void setFlag(String flag, String value, Player player) {
        if (flag.equals("neutral") && !MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality")) {
            player.sendMessage(ChatColor.RED + "" + Locale.get("NeutralityDisabled"));
            return;
        }

        if (!LocalConfigService.getInstance().getBoolean("factionsCanSetPrefixColors")) {
            player.sendMessage("Players can't set prefix colors.");
            return;
        }

        if (flag.equals("prefixColor") && (!MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes"))) {
            player.sendMessage(ChatColor.RED + "" + Locale.get("PrefixesDisabled"));
            return;
        }

        if (flag.equals("fiefsEnabled") && !FiefsIntegrator.getInstance().isFiefsPresent()) {
            player.sendMessage("Fiefs either isn't enabled or present.");
            return;
        }

        if (flag.equals("officersCanMintCurrency") && !CurrenciesIntegrator.getInstance().isCurrenciesPresent()) {
            // TODO: add locale message
            return;
        }

        if (isFlag(flag)) {
            if (integerValues.containsKey(flag)) {
                integerValues.replace(flag, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + Locale.get("IntegerSet"));
            } else if (booleanValues.containsKey(flag)) {
                booleanValues.replace(flag, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + Locale.get("BooleanSet"));
            } else if (doubleValues.containsKey(flag)) {
                doubleValues.replace(flag, Double.parseDouble(value));
                player.sendMessage(ChatColor.GREEN + Locale.get("DoubleSet"));
            } else if (stringValues.containsKey(flag)) {

                if (flag.equalsIgnoreCase("dynmapTerritoryColor")) {
                    String hex = value;
                    /*

                                            Hex Color Regex

                            This regex matches #FFF or #FFFFFF respectively.
                            Support values range from a-f/A-F & 0-9, giving
                                        full access to hex color.

                     */
                    if (!hex.matches("^#([A-Fa-f0-9]{6}|[A-Fa-f0-9]{3})$")) {
                        final String output = ColorConversion.attemptDecode(hex, false);
                        if (output == null) {
                            player.sendMessage("Please provide a valid hexadecimal color.");
                            // TODO Replace this with a new Locale message.
                            return;
                        } else hex = output;
                    }
                    stringValues.replace(flag, hex);
                    final Color awtColour = Color.decode(hex); // Convert to AWT Color.
                    player.sendMessage(
                            String.format("You have given the color R: %d, G: %d, B: %d",
                                    awtColour.getRed(),
                                    awtColour.getGreen(),
                                    awtColour.getBlue()
                            )
                    );
                    // TODO Replace this with a new Locale message.
                    return;
                }

                stringValues.replace(flag, value);
                player.sendMessage(ChatColor.GREEN + Locale.get("StringSet"));
            }

            if (flag.equals("dynmapTerritoryColor")) {
                DynmapIntegrator.getInstance().updateClaims(); // update dynmap to reflect color change
            }
        } else {
            player.sendMessage(ChatColor.RED + String.format(Locale.get("WasntFound"), flag));
        }
    }

    public Object getFlag(String flag) {
        if (!isFlag(flag)) {
            Logger.getInstance().debug(String.format("[DEBUG] Flag '%s' was not found!", flag));
            return false;
        }

        if (integerValues.containsKey(flag)) {
            Logger.getInstance().debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, integerValues.get(flag)));
            return integerValues.get(flag);
        } else if (booleanValues.containsKey(flag)) {
            Logger.getInstance().debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, booleanValues.get(flag)));
            return booleanValues.get(flag);
        } else if (doubleValues.containsKey(flag)) {
            Logger.getInstance().debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, doubleValues.get(flag)));
            return doubleValues.get(flag);
        } else if (stringValues.containsKey(flag)) {
            Logger.getInstance().debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, stringValues.get(flag)));
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
        StringBuilder toReturn = new StringBuilder();
        for (String flagName : flagNames) {

            if (flagName.equals("neutral") && !MedievalFactions.getInstance().getConfig().getBoolean("allowNeutrality")) {
                continue;
            }

            if (flagName.equals("prefixColor") && (!MedievalFactions.getInstance().getConfig().getBoolean("playersChatWithPrefixes") || !LocalConfigService.getInstance().getBoolean("factionsCanSetPrefixColors"))) {
                continue;
            }

            if (flagName.equals("fiefsEnabled") && !FiefsIntegrator.getInstance().isFiefsPresent()) {
                continue;
            }

            if (flagName.equals("officersCanMintCurrency") && !CurrenciesIntegrator.getInstance().isCurrenciesPresent()) {
                continue;
            }

            if (!toReturn.toString().equals("")) {
                toReturn.append(", ");
            }
            if (integerValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, integerValues.get(flagName)));
            } else if (booleanValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, booleanValues.get(flagName)));
            } else if (doubleValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, doubleValues.get(flagName)));
            } else if (stringValues.containsKey(flagName)) {
                toReturn.append(String.format("%s: %s", flagName, stringValues.get(flagName)));
            }
        }
        return toReturn.toString();
    }
}