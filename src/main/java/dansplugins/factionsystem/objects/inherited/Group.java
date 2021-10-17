package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.IGroup;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class Group implements IGroup {

    protected String name = "defaultName";
    protected String description = "defaultDescription";
    protected UUID owner = UUID.randomUUID();

    protected ArrayList<UUID> members = new ArrayList<>();
    protected ArrayList<UUID> officers = new ArrayList<>();

    private ArrayList<UUID> invited = new ArrayList<>();

    @Override
    public void setName(String newName) {
        name = newName;
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void setDescription(String newDesc) {
        description = newDesc;
    }

    @Override
    public String getDescription() {
        return description;
    }

    @Override
    public void setOwner(UUID UUID) {
        owner = UUID;
    }

    @Override
    public boolean isOwner(UUID UUID) {
        return owner.equals(UUID);
    }

    @Override
    public UUID getOwner() {
        return owner;
    }

    @Override
    public void addMember(UUID UUID) {
        members.add(UUID);
    }

    @Override
    public void removeMember(UUID UUID) {
        members.remove(UUID);
    }

    @Override
    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    @Override
    public ArrayList<UUID> getMemberList() {
        return members;
    }

    @Override
    public ArrayList<UUID> getMemberArrayList() {
        return members;
    }

    @Override
    public String getMemberListSeparatedByCommas() {
        ArrayList<UUID> uuids = getMemberList();
        String players = "";
        for(UUID uuid : uuids) {
            String playerName = UUIDChecker.getInstance().findPlayerNameBasedOnUUID(uuid);
            players += playerName + ", ";
        }
        if (players.length() > 0) {
            return players.substring(0, players.length() - 2);
        }
        return "";
    }

    @Override
    public boolean addOfficer(UUID newOfficer) {
        if (!officers.contains(newOfficer)) {
            officers.add(newOfficer);
            return true;
        }
        return false;
    }

    @Override
    public boolean removeOfficer(UUID officerToRemove) {
        return officers.remove(officerToRemove);
    }

    @Override
    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    @Override
    public int getNumOfficers() {
        return officers.size();
    }

    @Override
    public ArrayList<UUID> getOfficerList() {
        return officers;
    }

    @Override
    public int getPopulation() {
        return members.size();
    }

    @Override
    public void invite(UUID playerName) {
        Player player = getServer().getPlayer(playerName);
        if (player != null) {
            UUID playerUUID = getServer().getPlayer(playerName).getUniqueId();
            invited.add(playerUUID);
        }
    }

    @Override
    public void uninvite(UUID player) {
        invited.remove(player);
    }

    @Override
    public boolean isInvited(UUID uuid) {
        return invited.contains(uuid);
    }

}