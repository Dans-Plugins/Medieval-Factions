package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.FactionFlags;
import dansplugins.factionsystem.objects.Gate;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public interface IFaction {

// persistent lists -----------------

    // members
    void addMember(UUID UUID);
    void removeMember(UUID UUID);
    boolean isMember(UUID uuid);
    ArrayList<UUID> getMemberList();
    ArrayList<UUID> getMemberArrayList();
    String getMemberListSeparatedByCommas();
    int getPopulation();

    // officers
    boolean addOfficer(UUID newOfficer);
    boolean removeOfficer(UUID officerToRemove);
    boolean isOfficer(UUID uuid);
    int getNumOfficers();
    ArrayList<UUID> getOfficerList();

    // enemies
    void addEnemy(String factionName);
    void removeEnemy(String factionName);
    boolean isEnemy(String factionName);
    ArrayList<String> getEnemyFactions();
    String getEnemiesSeparatedByCommas();

    // allies
    void addAlly(String factionName);
    void removeAlly(String factionName);
    boolean isAlly(String factionName);
    ArrayList<String> getAllies();
    String getAlliesSeparatedByCommas();

    // laws
    void addLaw(String newLaw);
    boolean removeLaw(String lawToRemove);
    boolean removeLaw(int i);
    boolean editLaw(int i, String newString);
    int getNumLaws();
    ArrayList<String> getLaws();

    // vassalage
    boolean isVassal(String faction);
    boolean isLiege();
    boolean hasLiege();
    boolean isLiege(String faction);
    void addVassal(String factionName);
    void removeVassal(String faction);
    void clearVassals();
    int getNumVassals();
    ArrayList<String> getVassals();

    // gates
    void addGate(Gate gate);
    void removeGate(Gate gate);
    ArrayList<Gate> getGates();
    boolean hasGateTrigger(Block block);
    ArrayList<Gate> getGatesForTrigger(Block block);
    int getTotalGates();

// persistent strings  -----------------

    void setName(String newName);
    String getName();

    void setDescription(String newDesc);
    String getDescription();

    void setLiege(String newLiege);
    String getLiege();

    String getPrefix();
    void setPrefix(String newPrefix);

// other persistent types  -----------------

    void setOwner(UUID UUID);
    boolean isOwner(UUID UUID);
    UUID getOwner();

    void setFactionHome(Location l);
    Location getFactionHome();

    FactionFlags getFlags();

    int getBonusPower();
    void setBonusPower(int i);

// ephemeral lists -----------------

    void invite(UUID playerName);
    void uninvite(UUID player);
    boolean isInvited(UUID uuid);

    void requestTruce(String factionName);
    boolean isTruceRequested(String factionName);
    void removeRequestedTruce(String factionName);

    void requestAlly(String factionName);
    boolean isRequestedAlly(String factionName);
    void removeAllianceRequest(String factionName);

    String getVassalsSeparatedByCommas();
    void addAttemptedVassalization(String factionName);
    boolean hasBeenOfferedVassalization(String factionName);
    void removeAttemptedVassalization(String factionName);

// other ephemeral types

    void toggleAutoClaim();
    boolean getAutoClaimStatus();

// miscellaneous -----------------

    String getTopLiege();
    int calculateCumulativePowerLevelWithoutVassalContribution();
    int calculateCumulativePowerLevelWithVassalContribution();
    int getCumulativePowerLevel();
    int getMaximumCumulativePowerLevel();
    int calculateMaxOfficers();
    List<ClaimedChunk> getClaimedChunks();
    boolean isWeakened();
    void updateData(String oldName, String newName);

}