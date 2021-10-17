package dansplugins.factionsystem.objects.domain.specification;

import dansplugins.factionsystem.objects.inherited.specification.modifiers.Lockable;

public interface ILockedBlock extends Lockable {
    String getWorld();
    void setWorld(String name);
    int getX();
    int getY();
    int getZ();
    void setFaction(String s);
    String getFactionName();
}