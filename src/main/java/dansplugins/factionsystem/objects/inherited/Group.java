package dansplugins.factionsystem.objects.inherited;

import dansplugins.factionsystem.objects.inherited.specification.IGroup;

import java.util.ArrayList;
import java.util.UUID;

public class Group implements IGroup {

    @Override
    public void setName(String newName) {

    }

    @Override
    public String getName() {
        return null;
    }

    @Override
    public void setDescription(String newDesc) {

    }

    @Override
    public String getDescription() {
        return null;
    }

    @Override
    public void setOwner(UUID UUID) {

    }

    @Override
    public boolean isOwner(UUID UUID) {
        return false;
    }

    @Override
    public UUID getOwner() {
        return null;
    }

    @Override
    public void addMember(UUID UUID) {

    }

    @Override
    public void removeMember(UUID UUID) {

    }

    @Override
    public boolean isMember(UUID uuid) {
        return false;
    }

    @Override
    public ArrayList<UUID> getMemberList() {
        return null;
    }

    @Override
    public ArrayList<UUID> getMemberArrayList() {
        return null;
    }

    @Override
    public String getMemberListSeparatedByCommas() {
        return null;
    }

    @Override
    public boolean addOfficer(UUID newOfficer) {
        return false;
    }

    @Override
    public boolean removeOfficer(UUID officerToRemove) {
        return false;
    }

    @Override
    public boolean isOfficer(UUID uuid) {
        return false;
    }

    @Override
    public int getNumOfficers() {
        return 0;
    }

    @Override
    public ArrayList<UUID> getOfficerList() {
        return null;
    }

    @Override
    public int getPopulation() {
        return 0;
    }

    @Override
    public void invite(UUID playerName) {

    }

    @Override
    public void uninvite(UUID player) {

    }

    @Override
    public boolean isInvited(UUID uuid) {
        return false;
    }
}