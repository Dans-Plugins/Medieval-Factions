package dansplugins.factionsystem.objects.domain.specification;

import preponderous.ponder.modifiers.Savable;

import java.time.LocalDateTime;

public interface IWar extends Savable {
    String getAttacker();
    String getDefender();
    String getReason();
    LocalDateTime getDate();
    boolean isActive();
    void setActive(boolean active);
}