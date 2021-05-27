package dansplugins.factionsystem;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.Gate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

public class GateManager {
    private static GateManager instance;

    private GateManager() {

    }

    public static GateManager getInstance() {
        if (instance == null) {
            instance = new GateManager();
        }
        return instance;
    }

    public void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
            ClaimedChunk claim = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                    clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());

            if (faction.hasGateTrigger(clickedBlock)) {
                for (Gate g : faction.getGatesForTrigger(clickedBlock)) {
                    BlockData blockData = clickedBlock.getBlockData();
                    Powerable powerable = (Powerable) blockData;
                    if (powerable.isPowered()) {
                        if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                            g.openGate();
                        }
                        else {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                            return;
                        }
                    }
                    else {
                        if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                            g.closeGate();
                        }
                        else {
                            event.setCancelled(true);
                            player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                            return;
                        }
                    }
                }
                return;
            }
        }
    }

    public void handleCreatingGate(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CanOnlyCreateGatesInClaimedTerritory"));
            return;
        }
        else {
            ClaimedChunk claimedChunk = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(), clickedBlock.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
            if (claimedChunk != null) {
                if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeMemberToCreateGate"));
                    return;
                }
                else {
                    if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToCreateGate"));
                        return;
                    }
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info
            // of that existing gate instead of trying to create a new one.
            if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null) {
                Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Point1PlacementSuccessful"));
                    event.getPlayer().sendMessage(ChatColor.YELLOW + LocaleManager.getInstance().getText("ClickToPlaceSecondCorner"));
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MaterialsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("WorldsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CuboidDisallowed1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
                else {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacement1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
            }
            else if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                    if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                        event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("Point2PlacedSuccessfully"));
                        event.getPlayer().sendMessage(ChatColor.YELLOW + LocaleManager.getInstance().getText("ClickOnTriggerLever"));
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("MaterialsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("WorldsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CuboidDisallowed2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("ThreeBlockRequirement"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                    else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacement2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                }
            }
            else if (EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                    && !EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock)) {
                if (clickedBlock.getType().equals(Material.LEVER)) {
                    if (ChunkManager.getInstance().isClaimed(clickedBlock.getChunk(), PersistentData.getInstance().getClaimedChunks())) {
                        Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                        if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                            ClaimedChunk claim = ChunkManager.getInstance().getClaimedChunk(clickedBlock.getChunk().getX(), clickedBlock.getChunk().getZ(),
                                    clickedBlock.getChunk().getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
                            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());
                            faction.addGate(EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LeverSuccessfullyLinked"));
                            event.getPlayer().sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("GateCreated"));
                            return;
                        }
                        else {
                            event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CancelledGatePlacementErrorLinking"));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            return;
                        }
                    }
                    else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("OnlyUseLeversInClaimedTerritory"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        return;
                    }
                }
                else {
                    event.getPlayer().sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TriggerBlockNotLever"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    return;
                }
            }
        }
        else {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PermissionGate"));
        }
    }
}
