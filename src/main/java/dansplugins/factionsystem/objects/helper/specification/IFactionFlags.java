package dansplugins.factionsystem.objects.helper.specification;

import org.bukkit.entity.Player;

import java.util.HashMap;

public interface IFactionFlags {
    void initializeFlagValues();
    void loadMissingFlagsIfNecessary();
    void sendFlagList(Player player);
    void setFlag(String flag, String value, Player player);
    Object getFlag(String flag);
    HashMap<String, Integer> getIntegerValues();
    void setIntegerValues(HashMap<String, Integer> values);
    HashMap<String, Boolean> getBooleanValues();
    void setBooleanValues(HashMap<String, Boolean> values);
    HashMap<String, Double> getDoubleValues();
    void setDoubleValues(HashMap<String, Double> values);
    HashMap<String, String> getStringValues();
    void setStringValues(HashMap<String, String> values);
    int getNumFlags();
}