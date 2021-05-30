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

    public boolean getFlag(String flag) {
        if (!isFlag(flag)) {
            return false;
        }
        return flags.get(flag);
    }

    public void setFlag(String flag, boolean value) {
        // this should never cause an issue as we will sanitize user input
        flags.replace(flag, value);
    }

    public boolean isFlag(String flag) {
        // this method will likely need to be used to sanitize user input
        return flags.containsKey(flag);
    }

}