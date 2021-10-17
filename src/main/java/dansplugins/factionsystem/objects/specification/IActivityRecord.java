package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.specification.generic.PlayerRecord;

import java.time.ZonedDateTime;

public interface IActivityRecord extends PlayerRecord {
    void setPowerLost(int power);
    int getPowerLost();
    void incrementPowerLost();
    void setLastLogout(ZonedDateTime date);
    ZonedDateTime getLastLogout();
    void incrementLogins();
    int getLogins();
    int getMinutesSinceLastLogout();
    String getActiveSessionLength();
    String getTimeSinceLastLogout();
}
