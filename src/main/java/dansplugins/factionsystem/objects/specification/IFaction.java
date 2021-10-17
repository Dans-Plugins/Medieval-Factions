package dansplugins.factionsystem.objects.specification;

import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.FactionFlags;
import dansplugins.factionsystem.objects.Gate;
import dansplugins.factionsystem.objects.specification.generic.Diplomatic;
import dansplugins.factionsystem.objects.specification.generic.Feudal;
import dansplugins.factionsystem.objects.specification.generic.Group;
import dansplugins.factionsystem.objects.specification.generic.Nation;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public interface IFaction extends Nation, Feudal {

    // gates
    void addGate(Gate gate);
    void removeGate(Gate gate);
    ArrayList<Gate> getGates();
    boolean hasGateTrigger(Block block);
    ArrayList<Gate> getGatesForTrigger(Block block);
    int getTotalGates();

    // persistent strings
    String getPrefix();
    void setPrefix(String newPrefix);

    // other persistent types
    void setFactionHome(Location l);
    Location getFactionHome();
    FactionFlags getFlags();
    int getBonusPower();
    void setBonusPower(int i);

    // ephemeral types
    void toggleAutoClaim();
    boolean getAutoClaimStatus();

    // miscellaneous
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