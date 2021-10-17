package dansplugins.factionsystem.objects.domain.specification;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dansplugins.factionsystem.objects.domain.Gate;
import dansplugins.factionsystem.objects.helper.GateCoord;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;

import java.util.ArrayList;
import java.util.Map;

public interface IGate {
    enum GateStatus { READY, OPENING, CLOSING }
    enum ErrorCodeAddCoord { None, WorldMismatch, MaterialMismatch, NoCuboids, Oversized, LessThanThreeHigh }

    World getWorld();
    void setWorld(String worldName);
    boolean isIntersecting(Gate gate);
    int getTopLeftX();
    int getTopLeftY();
    int getTopLeftZ();
    int getBottomRightX();
    int getBottomRightY();
    int getBottomRightZ();
    int getTopLeftChunkX();
    int getTopLeftChunkZ();
    int getBottomRightChunkX();
    int getBottomRightChunkZ();
    String getName();
    void setName(String value);
    boolean isOpen();
    boolean isReady();
    boolean isClosed();
    String getStatus();
    GateCoord getTrigger();
    GateCoord getCoord1();
    GateCoord getCoord2();
    boolean isParallelToZ();
    boolean isParallelToX();
    ArrayList<Block> GateBlocks();
    boolean gateBlocksMatch(Material mat);
    ErrorCodeAddCoord addCoord(Block clickedBlock);
    int getDimX();
    int getDimY();
    int getDimZ();
    int getDimX(GateCoord first, GateCoord second);
    int getDimY(GateCoord first, GateCoord second);
    int getDimZ(GateCoord first, GateCoord second);
    void openGate();
    void closeGate();
    void fillGate();
    boolean hasBlock(Block targetBlock);
    String coordsToString();
}
