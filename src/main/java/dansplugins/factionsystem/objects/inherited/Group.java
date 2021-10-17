package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.IGroup;

import java.util.ArrayList;
import java.util.UUID;

public class Group implements IGroup {

    @Override
    public void setName(String newName) {
        // TODO: implement
    }

    @Override
    public String getName() {
        // TODO: implement
        return null;
    }

    @Override
    public void setDescription(String newDesc) {
        // TODO: implement
    }

    @Override
    public String getDescription() {
        // TODO: implement
        return null;
    }

    @Override
    public void setOwner(UUID UUID) {
        // TODO: implement
    }

    @Override
    public boolean isOwner(UUID UUID) {
        // TODO: implement
        return false;
    }

    @Override
    public UUID getOwner() {
        // TODO: implement
        return null;
    }

    @Override
    public void addMember(UUID UUID) {
        // TODO: implement
    }

    @Override
    public void removeMember(UUID UUID) {
        // TODO: implement
    }

    @Override
    public boolean isMember(UUID uuid) {
        // TODO: implement
        return false;
    }

    @Override
    public ArrayList<UUID> getMemberList() {
        // TODO: implement
        return null;
    }

    @Override
    public ArrayList<UUID> getMemberArrayList() {
        // TODO: implement
        return null;
    }

    @Override
    public String getMemberListSeparatedByCommas() {
        // TODO: implement
        return null;
    }

    @Override
    public boolean addOfficer(UUID newOfficer) {
        // TODO: implement
        return false;
    }

    @Override
    public boolean removeOfficer(UUID officerToRemove) {
        // TODO: implement
        return false;
    }

    @Override
    public boolean isOfficer(UUID uuid) {
        // TODO: implement
        return false;
    }

    @Override
    public int getNumOfficers() {
        // TODO: implement
        return 0;
    }

    @Override
    public ArrayList<UUID> getOfficerList() {
        // TODO: implement
        return null;
    }

    @Override
    public int getPopulation() {
        // TODO: implement
        return 0;
    }

    @Override
    public void invite(UUID playerName) {
        // TODO: implement

    }

    @Override
    public void uninvite(UUID player) {
        // TODO: implement

    }

    @Override
    public boolean isInvited(UUID uuid) {
        // TODO: implement
        return false;
    }
}