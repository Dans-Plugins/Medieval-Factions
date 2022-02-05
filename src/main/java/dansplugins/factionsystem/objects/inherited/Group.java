/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.objects.inherited;

import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class Group {
    protected String name = "defaultName";
    protected String description = "defaultDescription";
    protected UUID owner = UUID.randomUUID();

    protected ArrayList<UUID> members = new ArrayList<>();
    protected ArrayList<UUID> officers = new ArrayList<>();

    private ArrayList<UUID> invited = new ArrayList<>();

    public void setName(String newName) {
        name = newName;
    }

    public String getName() {
        return name;
    }

    public void setDescription(String newDesc) {
        description = newDesc;
    }

    public String getDescription() {
        return description;
    }

    public void setOwner(UUID UUID) {
        owner = UUID;
    }

    public boolean isOwner(UUID UUID) {
        return owner.equals(UUID);
    }

    public UUID getOwner() {
        return owner;
    }

    public void addMember(UUID UUID) {
        members.add(UUID);
    }

    public void removeMember(UUID UUID) {
        members.remove(UUID);
    }

    public boolean isMember(UUID uuid) {
        return members.contains(uuid);
    }

    public ArrayList<UUID> getMemberList() {
        return members;
    }

    public ArrayList<UUID> getMemberArrayList() {
        return members;
    }

    public String getMemberListSeparatedByCommas() {
        ArrayList<UUID> uuids = getMemberList();
        String players = "";
        for(UUID uuid : uuids) {
            UUIDChecker uuidChecker = new UUIDChecker();
            String playerName = uuidChecker.findPlayerNameBasedOnUUID(uuid);
            players += playerName + ", ";
        }
        if (players.length() > 0) {
            return players.substring(0, players.length() - 2);
        }
        return "";
    }

    public boolean addOfficer(UUID newOfficer) {
        if (!officers.contains(newOfficer)) {
            officers.add(newOfficer);
            return true;
        }
        return false;
    }

    public boolean removeOfficer(UUID officerToRemove) {
        return officers.remove(officerToRemove);
    }

    public boolean isOfficer(UUID uuid) {
        return officers.contains(uuid);
    }

    public int getNumOfficers() {
        return officers.size();
    }

    public ArrayList<UUID> getOfficerList() {
        return officers;
    }

    public int getPopulation() {
        return members.size();
    }

    public void invite(UUID playerName) {
        Player player = getServer().getPlayer(playerName);
        if (player != null) {
            UUID playerUUID = getServer().getPlayer(playerName).getUniqueId();
            invited.add(playerUUID);
        }
    }

    public void uninvite(UUID player) {
        invited.remove(player);
    }

    public boolean isInvited(UUID uuid) {
        return invited.contains(uuid);
    }
}