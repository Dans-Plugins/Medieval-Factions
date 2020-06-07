package factionsystem;

public class PlayerPowerRecord {
    private String playerName = "";
    private int powerLevel = 0;
    private Faction playerFaction = null;

    public PlayerPowerRecord(String nameOfPlayer) {
        playerName = nameOfPlayer;
        powerLevel = 10;
    }

    public void setPlayerName(String newName) {
        playerName = newName;
    }

    public String getPlayerName() {
        return playerName;
    }

    public void setFaction(Faction faction) {
        playerFaction = faction;
    }

    public Faction getFaction() {
        return playerFaction;
    }

    public boolean increasePower() {
        if (powerLevel < 10) {
            powerLevel++;
            return true;
        }
        else {
            return false;
        }
    }

    public boolean decreasePower() {
        if (powerLevel > 0) {
            powerLevel--;
            return true;
        }
        else {
            return false;
        }
    }
}
