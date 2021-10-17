package dansplugins.factionsystem.objects.specification;

import java.util.ArrayList;
import java.util.UUID;

public interface IGroup {
    void setName(String newName);
    String getName();
    void setDescription(String newDesc);
    String getDescription();
    void setOwner(UUID UUID);
    boolean isOwner(UUID UUID);
    UUID getOwner();
    void addMember(UUID UUID);
    void removeMember(UUID UUID);
    boolean isMember(UUID uuid);
    ArrayList<UUID> getMemberList();
    ArrayList<UUID> getMemberArrayList();
    String getMemberListSeparatedByCommas();
    int getPopulation();
    void invite(UUID playerName);
    void uninvite(UUID player);
    boolean isInvited(UUID uuid);
}
