/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.helper;

import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.ColorConversion;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Daniel McCoy Stephenson
 * In order to add a new faction flag to this class, the following methods need to be altered:
 * - initializeFlagNames()
 * - initializeFlagValues()
 * - loadMissingFlagsIfNecessary()
 */
public class FactionFlags {
    private final ConfigService configService;
    private final LocaleService localeService;
    private final FiefsIntegrator fiefsIntegrator;
    private final CurrenciesIntegrator currenciesIntegrator;
    private final DynmapIntegrator dynmapIntegrator;
    private final Logger logger;

    private final ArrayList<String> flagNames = new ArrayList<>();
    private HashMap<String, Integer> integerValues = new HashMap<>();
    private HashMap<String, Boolean> booleanValues = new HashMap<>();
    private HashMap<String, Double> doubleValues = new HashMap<>();
    private HashMap<String, String> stringValues = new HashMap<>();

    public FactionFlags(ConfigService configService, LocaleService localeService, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, DynmapIntegrator dynmapIntegrator, Logger logger) {
        this.configService = configService;
        this.localeService = localeService;
        this.fiefsIntegrator = fiefsIntegrator;
        this.currenciesIntegrator = currenciesIntegrator;
        this.dynmapIntegrator = dynmapIntegrator;
        this.logger = logger;
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
        booleanValues.put("alliesCanInteractWithLand", configService.getBoolean("allowAllyInteraction"));
        booleanValues.put("vassalageTreeCanInteractWithLand", configService.getBoolean("allowVassalageTreeInteraction"));
        booleanValues.put("neutral", false);
        stringValues.put("dynmapTerritoryColor", "#ff0000");
        stringValues.put("territoryAlertColor", configService.getString("territoryAlertColor"));
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
            booleanValues.put("alliesCanInteractWithLand", configService.getBoolean("allowAllyInteraction"));
        }
        if (!booleanValues.containsKey("vassalageTreeCanInteractWithLand")) {
            booleanValues.put("vassalageTreeCanInteractWithLand", configService.getBoolean("allowVassalageTreeInteraction"));
        }
        if (!booleanValues.containsKey("neutral")) {
            booleanValues.put("neutral", false);
        }
        if (!stringValues.containsKey("dynmapTerritoryColor")) {
            stringValues.put("dynmapTerritoryColor", "#ff0000");
        }
        if (!stringValues.containsKey("territoryAlertColor")) {
            stringValues.put("territoryAlertColor", configService.getString("territoryAlertColor"));
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
        if (flag.equals("neutral") && !configService.getBoolean("allowNeutrality")) {
            player.sendMessage(ChatColor.RED + "" + localeService.get("NeutralityDisabled"));
            return;
        }

        if (flag.equals("prefixColor") && !configService.getBoolean("factionsCanSetPrefixColors")) {
            player.sendMessage("Players can't set prefix colors.");
            return;
        }

        if (flag.equals("prefixColor") && (!configService.getBoolean("playersChatWithPrefixes"))) {
            player.sendMessage(ChatColor.RED + "" + localeService.get("PrefixesDisabled"));
            return;
        }

        if (flag.equals("fiefsEnabled") && !fiefsIntegrator.isFiefsPresent()) {
            player.sendMessage("Fiefs either isn't enabled or present.");
            return;
        }

        if (flag.equals("officersCanMintCurrency") && !currenciesIntegrator.isCurrenciesPresent()) {
            // TODO: add locale message
            return;
        }

        if (isFlag(flag)) {
            if (integerValues.containsKey(flag)) {
                integerValues.replace(flag, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + localeService.get("IntegerSet"));
            } else if (booleanValues.containsKey(flag)) {
                booleanValues.replace(flag, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + localeService.get("BooleanSet"));
            } else if (doubleValues.containsKey(flag)) {
                doubleValues.replace(flag, Double.parseDouble(value));
                player.sendMessage(ChatColor.GREEN + localeService.get("DoubleSet"));
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
                player.sendMessage(ChatColor.GREEN + localeService.get("StringSet"));
            }

            if (flag.equals("dynmapTerritoryColor")) {
                dynmapIntegrator.updateClaims(); // update dynmap to reflect color change
            }
        } else {
            player.sendMessage(ChatColor.RED + String.format(localeService.get("WasntFound"), flag));
        }
    }

    public Object getFlag(String flag) {
        if (!isFlag(flag)) {
            logger.debug(String.format("[DEBUG] Flag '%s' was not found!", flag));
            return false;
        }

        if (integerValues.containsKey(flag)) {
            logger.debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, integerValues.get(flag)));
            return integerValues.get(flag);
        } else if (booleanValues.containsKey(flag)) {
            logger.debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, booleanValues.get(flag)));
            return booleanValues.get(flag);
        } else if (doubleValues.containsKey(flag)) {
            logger.debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, doubleValues.get(flag)));
            return doubleValues.get(flag);
        } else if (stringValues.containsKey(flag)) {
            logger.debug(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, stringValues.get(flag)));
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

            if (flagName.equals("neutral") && !configService.getBoolean("allowNeutrality")) {
                continue;
            }

            if (flagName.equals("prefixColor") && (!configService.getBoolean("playersChatWithPrefixes") || !configService.getBoolean("factionsCanSetPrefixColors"))) {
                continue;
            }

            if (flagName.equals("fiefsEnabled") && !fiefsIntegrator.isFiefsPresent()) {
                continue;
            }

            if (flagName.equals("officersCanMintCurrency") && !currenciesIntegrator.isCurrenciesPresent()) {
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