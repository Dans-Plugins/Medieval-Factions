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

public interface IFaction extends IGroup {

// persistent lists -----------------

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

    void setLiege(String newLiege);
    String getLiege();

    String getPrefix();
    void setPrefix(String newPrefix);

// other persistent types  -----------------

    void setFactionHome(Location l);
    Location getFactionHome();

    FactionFlags getFlags();

    int getBonusPower();
    void setBonusPower(int i);

// ephemeral lists -----------------

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