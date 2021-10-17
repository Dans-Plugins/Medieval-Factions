package dansplugins.factionsystem.objects.domain.specification;

import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Gate;
import dansplugins.factionsystem.objects.helper.FactionFlags;
import org.bukkit.Location;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.List;

public interface IFaction {

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