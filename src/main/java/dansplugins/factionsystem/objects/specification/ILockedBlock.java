package dansplugins.factionsystem.objects.specification;

import java.util.ArrayList;
import java.util.UUID;

public interface ILockedBlock {
    String getWorld();
    void setWorld(String name);
    int getX();
    int getY();
    int getZ();
    void setOwner(UUID s);
    UUID getOwner();
    void addToAccessList(UUID playerName);
    void removeFromAccessList(UUID playerName);
    boolean hasAccess(UUID playerName);
    ArrayList<UUID> getAccessList();
}
