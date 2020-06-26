package factionsystem.Objects;

import java.util.ArrayList;

public class LockedBlock {

    private String owner = "";
    private String factionName = "";
    private ArrayList<String> accessList = new ArrayList<>();
    private int x = 0;
    private int y = 0;
    private int z = 0;

    public LockedBlock(String o, String f, int newX, int newY, int newZ) {
        owner = o;
        factionName = f;
        x = newX;
        y = newY;
        z = newZ;
        accessList.add(owner);
    }

    public void setOwner(String s) {
        owner = s;
    }

    public String getOwner() {
        return owner;
    }

    public void setFaction(String s) {
        factionName = s;
    }

    public String getFactionName() {
        return factionName;
    }

    public void addToAccessList(String playerName) {
        if (!accessList.contains(playerName)) {
            accessList.add(playerName);
        }
    }

    public void removeFromAccessList(String playerName) {
        if (accessList.contains(playerName)) {
            accessList.remove(playerName);
        }
    }

    public boolean hasAccess(String playerName) {
        return accessList.contains(playerName);
    }

    public ArrayList<String> getAccessList() {
        return accessList;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getZ() {
        return z;
    }

}
