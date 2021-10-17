package dansplugins.factionsystem.objects.domain.specification;

import dansplugins.factionsystem.objects.inherited.specification.IPlayerRecord;

public interface IPowerRecord extends IPlayerRecord {
    int maxPower();
    boolean increasePower();
    boolean decreasePower();
    int getPowerLevel();
    void setPowerLevel(int newPower);
    boolean increasePowerByTenPercent();
    void decreasePowerByTenPercent();
}