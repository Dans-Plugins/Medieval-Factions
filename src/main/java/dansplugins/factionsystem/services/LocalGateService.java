/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;

/**
 * @author Caibinus
 * @author Daniel McCoy Stephenson
 */
public class LocalGateService {
    private static LocalGateService instance;

    private LocalGateService() {

    }

    public static LocalGateService getInstance() {
        if (instance == null) {
            instance = new LocalGateService();
        }
        return instance;
    }

    public void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!PersistentData.getInstance().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            return;
        }

        ClaimedChunk claim = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
        Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());
        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (!faction.getName().equals(playersFaction.getName())) {
            return;
        }

        if (!faction.hasGateTrigger(clickedBlock)) {
            return;
        }

        for (Gate g : faction.getGatesForTrigger(clickedBlock)) {
            BlockData blockData = clickedBlock.getBlockData();
            Powerable powerable = (Powerable) blockData;
            if (powerable.isPowered()) {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    g.openGate();
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                    return;
                }
            } else {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    g.closeGate();
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("PleaseWaitGate"), g.getStatus()));
                    return;
                }
            }
        }
    }

    public void handlePotentialGateInteraction(Block block, BlockRedstoneEvent event) {
        if (PersistentData.getInstance().getChunkDataAccessor().isClaimed(block.getChunk())) {
            ClaimedChunk claim = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(block.getChunk());
            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());

            if (faction.hasGateTrigger(block)) {
                for (Gate g : faction.getGatesForTrigger(block)) {
                    BlockData blockData = block.getBlockData();
                    Powerable powerable = (Powerable) blockData;
                    if (powerable.isPowered()) {
                        if (faction.getGatesForTrigger(block).get(0).isReady()) {
                            g.openGate();
                        } else {
                            return;
                        }
                    } else {
                        if (faction.getGatesForTrigger(block).get(0).isReady()) {
                            g.closeGate();
                        } else {
                            return;
                        }
                    }
                }
            }
        }
    }

    public void handleCreatingGate(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!PersistentData.getInstance().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CanOnlyCreateGatesInClaimedTerritory"));
            return;
        } else {
            ClaimedChunk claimedChunk = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
            if (claimedChunk != null) {
                if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertMustBeMemberToCreateGate"));
                    return;
                } else {
                    if (!PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !PersistentData.getInstance().getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertMustBeOwnerOrOfficerToCreateGate"));
                        return;
                    }
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info of that existing gate instead of trying to create a new one.
            if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null) {
                Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("Point1PlacementSuccessful"));
                    event.getPlayer().sendMessage(ChatColor.YELLOW + LocalLocaleService.getInstance().getText("ClickToPlaceSecondCorner"));
                } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("MaterialsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("WorldsMismatch1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                    event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CuboidDisallowed1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CancelledGatePlacement1"));
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            } else if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                    if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                        event.getPlayer().sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("Point2PlacedSuccessfully"));
                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Click on the trigger block...");
                    } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("MaterialsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("WorldsMismatch2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CuboidDisallowed2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh)) {
                        event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("ThreeBlockRequirement"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CancelledGatePlacement2"));
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                }
            } else if (EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                    && EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                    && !EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock)) {
                if (clickedBlock.getBlockData() instanceof Powerable) {
                    if (PersistentData.getInstance().getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
                        Gate.ErrorCodeAddCoord e = EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                        if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                            ClaimedChunk claim = PersistentData.getInstance().getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
                            Faction faction = PersistentData.getInstance().getFaction(claim.getHolder());
                            faction.addGate(EphemeralData.getInstance().getCreatingGatePlayers().get(event.getPlayer().getUniqueId()));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 4/4: Trigger successfully linked.");
                            event.getPlayer().sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("GateCreated"));
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("CancelledGatePlacementErrorLinking"));
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        }
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Error: Can only use triggers in claimed territory.");
                        EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "Trigger block was not powerable. Cancelled gate placement.");
                    EphemeralData.getInstance().getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("PermissionGate"));
        }
    }
}
