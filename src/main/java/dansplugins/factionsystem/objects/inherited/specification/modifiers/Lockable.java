package dansplugins.factionsystem.objects.inherited.specification.modifiers;

import java.util.ArrayList;
import java.util.UUID;

public interface Lockable {
    void setOwner(UUID s);
    UUID getOwner();
    void addToAccessList(UUID playerName);
    void removeFromAccessList(UUID playerName);
    boolean hasAccess(UUID playerName);
    ArrayList<UUID> getAccessList();
}