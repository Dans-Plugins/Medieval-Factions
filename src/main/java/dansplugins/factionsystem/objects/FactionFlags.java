package dansplugins.factionsystem.objects;

import java.util.HashMap;

public class FactionFlags {

    private HashMap<String, Boolean> flags = new HashMap<>();

    public int getNumFlags() {
        return flags.size();
    }

    public void initializeFlags() {
        flags.put("TestFlag", true);
    }

}