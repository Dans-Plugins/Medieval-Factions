package dansplugins.factionsystem.objects.domain.specification;

public interface IPowerRecord {
    int maxPower();
    boolean increasePower();
    boolean decreasePower();
    int getPowerLevel();
    void setPowerLevel(int newPower);
    boolean increasePowerByTenPercent();
    int decreasePowerByTenPercent();
}