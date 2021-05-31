package dansplugins.factionsystem.objects;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.managers.LocaleManager;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.HashMap;

public class FactionFlags {

    private ArrayList<String> flagNames = new ArrayList<>();
    private HashMap<String, Boolean> flagValues = new HashMap<>();

    public FactionFlags() {
        initializeFlagNames();
    }

    public int getNumFlags() {
        return flagValues.size();
    }

    private void initializeFlagNames() { // this is called internally
        flagNames.add("officerRankRequiredToClaimLand");
    }

    public void initializeFlagValues() { // this is called externally in Faction.java
        flagValues.put("officerRankRequiredToClaimLand", true);
    }

    public void loadMissingFlagsIfNecessary() {
        if (!flagValues.containsKey("officerRankRequiredToClaimLand")) {
            flagValues.put("officerRankRequiredToClaimLand", true);
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
            return false;
        }
        return flagValues.get(flag);
    }

    public ArrayList<String> getFlagNames() {
        return flagNames;
    }

    public HashMap<String, Boolean> getFlagValues() {
        return flagValues;
    }

    public void setFlagNames(ArrayList<String> names) {
        flagNames = names;
    }

    public void setFlagValues(HashMap<String, Boolean> values) {
        flagValues = values;
    }

    private boolean isFlag(String flag) {
        // this method will likely need to be used to sanitize user input
        return flagNames.contains(flag);
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