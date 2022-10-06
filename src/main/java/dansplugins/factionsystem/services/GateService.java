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

import java.util.Objects;

/**
 * @author Caibinus
 */
public class GateService {
    private final PersistentData persistentData;
    private final LocaleService localeService;
    private final EphemeralData ephemeralData;
    private final PlayerService playerService;
    private final MessageService messageService;

    public GateService(PersistentData persistentData, LocaleService localeService, EphemeralData ephemeralData, PlayerService playerService, MessageService messageService) {
        this.persistentData = persistentData;
        this.localeService = localeService;
        this.ephemeralData = ephemeralData;
        this.playerService = playerService;
        this.messageService = messageService;
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
                    playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus())
                            , Objects.requireNonNull(messageService.getLanguage().getString("PleaseWaitGate")).replace("#status#", g.getStatus()), true);
                    return;
                }
            } else {
                if (faction.getGatesForTrigger(clickedBlock).get(0).isReady()) {
                    g.closeGate();
                } else {
                    event.setCancelled(true);
                    playerService.sendMessage(player, ChatColor.RED + String.format(localeService.get("PleaseWaitGate"), g.getStatus())
                            , Objects.requireNonNull(messageService.getLanguage().getString("PleaseWaitGate")).replace("#status#", g.getStatus()), true);
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
            playerService.sendMessage(player, ChatColor.RED + localeService.get("CanOnlyCreateGatesInClaimedTerritory"), "CanOnlyCreateGatesInClaimedTerritory", false);
            return;
        } else {
            ClaimedChunk claimedChunk = persistentData.getChunkDataAccessor().getClaimedChunk(clickedBlock.getChunk());
            if (claimedChunk != null) {
                if (!persistentData.getFaction(claimedChunk.getHolder()).isMember(player.getUniqueId())) {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMustBeMemberToCreateGate"), "AlertMustBeMemberToCreateGate", false);

                    return;
                } else {
                    if (!persistentData.getFaction(claimedChunk.getHolder()).isOwner(player.getUniqueId())
                            && !persistentData.getFaction(claimedChunk.getHolder()).isOfficer(player.getUniqueId())) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("AlertMustBeOwnerOrOfficerToCreateGate"), "AlertMustBeOwnerOrOfficerToCreateGate", false);
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
                    playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("Point1PlacementSuccessful")
                            , "Point1PlacementSuccessful", false);
                    playerService.sendMessage(event.getPlayer(), ChatColor.YELLOW + localeService.get("ClickToPlaceSecondCorner")
                            , "ClickToPlaceSecondCorner", false);
                } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("MaterialsMismatch1")
                            , "MaterialsMismatch1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("WorldsMismatch1")
                            , "WorldsMismatch1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("CuboidDisallowed1")
                            , "CuboidDisallowed1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                } else {
                    playerService.sendMessage(player, ChatColor.RED + localeService.get("CancelledGatePlacement1")
                            , "CancelledGatePlacement1", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            } else if (ephemeralData.getCreatingGatePlayers().containsKey(event.getPlayer().getUniqueId()) && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1() != null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord2() == null
                    && ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getTrigger() == null) {
                if (!ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).getCoord1().equals(clickedBlock)) {
                    Gate.ErrorCodeAddCoord e = ephemeralData.getCreatingGatePlayers().get(event.getPlayer().getUniqueId()).addCoord(clickedBlock);
                    if (e.equals(Gate.ErrorCodeAddCoord.None)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("Point2PlacementSuccessful")
                                , "Point2PlacementSuccessful", false);
                        playerService.sendMessage(event.getPlayer(), ChatColor.YELLOW + "Click on the trigger block..."
                                , "ClickTBlock", false);
                    } else if (e.equals(Gate.ErrorCodeAddCoord.MaterialMismatch)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("MaterialsMismatch2")
                                , "MaterialsMismatch2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.WorldMismatch)) {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("WorldsMismatch2")
                                , "WorldsMismatch2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.NoCuboids)) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("CuboidDisallowed2")
                                , "CuboidDisallowed2", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else if (e.equals(Gate.ErrorCodeAddCoord.LessThanThreeHigh)) {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("ThreeBlockRequirement")
                                , "ThreeBlockRequirement", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    } else {
                        playerService.sendMessage(player, ChatColor.RED + localeService.get("CancelledGatePlacement2")
                                , "CancelledGatePlacement2", false);
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
                            playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + "Creating Gate 4/4: Trigger successfully linked."
                                    , "Point4TriggeredSuccessfully", false);
                            playerService.sendMessage(event.getPlayer(), ChatColor.GREEN + localeService.get("GateCreated")
                                    , "GateCreated", false);
                        } else {
                            playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("CancelledGatePlacementErrorLinking")
                                    , "CancelledGatePlacementErrorLinking", false);
                            ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                        }
                    } else {
                        playerService.sendMessage(event.getPlayer(), ChatColor.RED + "Error: Can only use triggers in claimed territory."
                                , "CanOnlyTrigger", false);
                        ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                    }
                } else {
                    playerService.sendMessage(event.getPlayer(), ChatColor.RED + "Trigger block was not powerable. Cancelled gate placement."
                            , "TriggerBlockNotPowerable", false);
                    ephemeralData.getCreatingGatePlayers().remove(event.getPlayer().getUniqueId());
                }
            }
        } else {
            playerService.sendMessage(event.getPlayer(), ChatColor.RED + localeService.get("PermissionGate")
                    , Objects.requireNonNull(messageService.getLanguage().getString("PermissionNeeded")).replace("#permission#", "mf.gate"), true);
        }
    }
}
