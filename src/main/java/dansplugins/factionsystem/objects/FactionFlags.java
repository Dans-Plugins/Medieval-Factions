package dansplugins.factionsystem.objects;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.managers.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class FactionFlags {

    private final boolean debug = true;

    private ArrayList<String> flagNames = new ArrayList<>();
    private HashMap<String, Boolean> flagValues = new HashMap<>();

    public FactionFlags() {
        initializeFlagNames();
    }

    private void initializeFlagNames() { // this is called internally
        flagNames.add("mustBeOfficerToManageLand");
        flagNames.add("mustBeOfficerToInviteOthers");
        flagNames.add("alliesCanInteractWithLand");
        flagNames.add("vassalageTreeCanInteractWithLand");
    }

    public void initializeFlagValues() {
        // this is called externally in Faction.java when a faction is created in-game
        flagValues.put("mustBeOfficerToManageLand", true);
        flagValues.put("mustBeOfficerToInviteOthers", true);
        flagValues.put("alliesCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowAllyInteraction"));
        flagValues.put("vassalageTreeCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowVassalageTreeInteraction"));
    }

    public void loadMissingFlagsIfNecessary() {
        // this is called externally in Faction.java when a faction is loaded from save files
        if (!flagValues.containsKey("mustBeOfficerToManageLand")) {
            flagValues.put("mustBeOfficerToManageLand", true);
        }
        if (!flagValues.containsKey("mustBeOfficerToInviteOthers")) {
            flagValues.put("mustBeOfficerToInviteOthers", true);
        }
        if (!flagValues.containsKey("alliesCanInteractWithLand")) {
            flagValues.put("alliesCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowAllyInteraction"));
        }
        if (!flagValues.containsKey("vassalageTreeCanInteractWithLand")) {
            flagValues.put("vassalageTreeCanInteractWithLand", MedievalFactions.getInstance().getConfig().getBoolean("allowVassalageTreeInteraction"));
        }
    }

    public void sendFlagList(Player player) {
        player.sendMessage(ChatColor.AQUA + "" + getFlagsSeparatedByCommas());
    }


    public void setFlag(String flag, String value, Player player) {
        if (isFlag(flag)) {
            flagValues.replace(flag, Boolean.parseBoolean(value));
            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("BooleanSet"));
        }
        else {
            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("WasntFound"), flag));
        }
    }

    public boolean getFlag(String flag) {
        if (!isFlag(flag)) {
            if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was not found!", flag)); }
            return false;
        }
        if (debug) { System.out.println(String.format("[DEBUG] Flag '%s' was found! Value: '%s'", flag, flagValues.get(flag))); }
        return flagValues.get(flag);
    }

    public HashMap<String, Boolean> getFlagValues() {
        return flagValues;
    }

    public void setFlagValues(HashMap<String, Boolean> values) {
        flagValues = values;
    }

    private boolean isFlag(String flag) {
        // this method will likely need to be used to sanitize user input
        return flagNames.contains(flag);
    }

    public int getNumFlags() {
        return flagValues.size();
    }

    private String getFlagsSeparatedByCommas() {
        String toReturn = "";
        for (String flagName : flagNames) {
            if (!toReturn.equals("")) {
                toReturn += ", ";
            }
            toReturn += String.format("%s: %s", flagName, flagValues.get(flagName));
        }
        return toReturn;
    }

}