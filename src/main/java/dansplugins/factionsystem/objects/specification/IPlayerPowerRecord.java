package dansplugins.factionsystem.objects.specification;

import java.util.UUID;

public interface IPlayerPowerRecord {
    int maxPower();
    void setPlayerUUID(UUID UUID);
    UUID getPlayerUUID();
    boolean increasePower();
    boolean decreasePower();
    int getPowerLevel();
    void setPowerLevel(int newPower);
    boolean increasePowerByTenPercent();
    void decreasePowerByTenPercent();
}