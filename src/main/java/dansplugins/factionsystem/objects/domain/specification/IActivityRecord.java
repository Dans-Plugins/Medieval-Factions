package dansplugins.factionsystem.objects.domain.specification;

import dansplugins.factionsystem.objects.inherited.specification.IPlayerRecord;

import java.time.ZonedDateTime;

public interface IActivityRecord {
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
