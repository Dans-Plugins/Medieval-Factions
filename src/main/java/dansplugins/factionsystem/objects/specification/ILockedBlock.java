package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.specification.generic.Lockable;

public interface ILockedBlock extends Lockable {
    String getWorld();
    void setWorld(String name);
    int getX();
    int getY();
    int getZ();
    void setFaction(String s);
    String getFactionName();
}