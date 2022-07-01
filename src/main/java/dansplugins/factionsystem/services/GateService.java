/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.services;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;
import org.bukkit.ChatColor;
import org.bukkit.block.Block;
import org.bukkit.block.data.BlockData;
import org.bukkit.block.data.Powerable;
import org.bukkit.entity.Player;
import org.bukkit.event.block.BlockRedstoneEvent;
import org.bukkit.event.player.PlayerInteractEvent;

/**
 * @author Caibinus
 */
public class GateService {
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final EphemeralData ephemeralData;

    public GateService(PersistentData persistentData, LocaleService localeService, EphemeralData ephemeralData) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
    }

    public void handlePotentialGateInteraction(Block clickedBlock, Player player, PlayerInteractEvent event) {
        if (!persistentData.getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            return;
        }

        ClaimedChunk claim = persistentData.getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
        Faction faction = persistentData.getFaction(claim.getHolder());
        Faction playersFaction = persistentData.getPlayersFaction(player.getUniqueId());

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
                    player.sendMessage(ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus()));
                    return;
                }
            } else {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    g.closeGate();
                } else {
                    event.setCancelled(true);
                    player.sendMessage(ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus()));
                    return;
                }
            }
        }
    }

    public void handlePotentialGateInteraction(Block block, BlockRedstoneEvent event) {
        if (persistentData.getChunkDataAccessor().isClaimed(block.getChunk())) {
            ClaimedChunk claim = persistentData.getChunkDataAccessor().getClaimedChunk(block.getChunk());
            Faction faction = persistentData.getFaction(claim.getHolder());

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
        if (!persistentData.getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
            player.sendMessage(ChatColor.RED + localeService.get("CanOnlyCreateGatesInClaimedTerritory"));
            return;
        } else {
            ClaimedChunk claimedChunk = persistentData.getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
            if (claimedChunk != null) {
                if (!persistentData.getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    player.sendMessage(ChatColor.RED + localeService.get("AlertMustBeMemberToCreateGate"));
                    return;
                } else {
                    if (!persistentData.getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !persistentData.getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        player.sendMessage(ChatColor.RED + localeService.get("AlertMustBeOwnerOrOfficerToCreateGate"));
                        return;
                    }
                }
            }
        }

        if (player.hasPermission("mf.gate")) {
            // TODO: Check if a gate already exists here, and if it does, print out some info of that existing gate instead of trying to create a new one.
            if (ephemeralData.getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() == null) {
                Gate.ErrorCodeAddCoord e = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                    event.getPlayer().sendMessage(ChatColor.GREEN + localeService.get("Point1PlacementSuccessful"));
                    event.getPlayer().sendMessage(ChatColor.YELLOW + localeService.get("ClickToPlaceSecondCorner"));
                } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + localeService.get("MaterialsMismatch1"));
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                    event.getPlayer().sendMessage(ChatColor.RED + localeService.get("WorldsMismatch1"));
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                    event.getPlayer().sendMessage(ChatColor.RED + localeService.get("CuboidDisallowed1"));
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + localeService.get("CancelledGatePlacement1"));
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            } else if (ephemeralData.getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate.ErrorCodeAddCoord e = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                    if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                        event.getPlayer().sendMessage(ChatColor.GREEN + localeService.get("Point2PlacedSuccessfully"));
                        event.getPlayer().sendMessage(ChatColor.YELLOW + "Click on the trigger block...");
                    } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + localeService.get("MaterialsMismatch2"));
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                        event.getPlayer().sendMessage(ChatColor.RED + localeService.get("WorldsMismatch2"));
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                        event.getPlayer().sendMessage(ChatColor.RED + localeService.get("CuboidDisallowed2"));
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh)) {
                        event.getPlayer().sendMessage(ChatColor.RED + localeService.get("ThreeBlockRequirement"));
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + localeService.get("CancelledGatePlacement2"));
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                }
            } else if (ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() != null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null
                    && !ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2().equals(clickedBlock)) {
                if (clickedBlock.getBlockData() instanceof Powerable) {
                    if (persistentData.getChunkDataAccessor().isClaimed(clickedBlock.getChunk())) {
                        Gate.ErrorCodeAddCoord e = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                        if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                            ClaimedChunk claim = persistentData.getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
                            Faction faction = persistentData.getFaction(claim.getHolder());
                            faction.addGate(ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()));
                            ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                            event.getPlayer().sendMessage(ChatColor.GREEN + "Creating Gate 4/4: Trigger successfully linked.");
                            event.getPlayer().sendMessage(ChatColor.GREEN + localeService.get("GateCreated"));
                        } else {
                            event.getPlayer().sendMessage(ChatColor.RED + localeService.get("CancelledGatePlacementErrorLinking"));
                            ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        }
                    } else {
                        event.getPlayer().sendMessage(ChatColor.RED + "Error: Can only use triggers in claimed territory.");
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                } else {
                    event.getPlayer().sendMessage(ChatColor.RED + "Trigger block was not powerable. Cancelled gate placement.");
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            }
        } else {
            player.sendMessage(ChatColor.RED + localeService.get("PermissionGate"));
        }
    }
}
