package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.specification.generic.PlayerRecord;

public interface IPlayerPowerRecord extends PlayerRecord {
    int maxPower();
    boolean increasePower();
    boolean decreasePower();
    int getPowerLevel();
    void setPowerLevel(int newPower);
    boolean increasePowerByTenPercent();
    void decreasePowerByTenPercent();
}