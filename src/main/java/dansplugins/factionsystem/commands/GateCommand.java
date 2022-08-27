/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class GateCommand extends SubCommand {
    private final MedievalFactions medievalFactions;

    public GateCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "gate", "gt", LOCALE_PREFIX + "CmdGate"
        }, true, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
        this.medievalFactions = medievalFactions;
    }

    /**
     * Method to execute the command for a player.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the sub-command (e.g. Ally).
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        if (!(checkPermissions(player, "mf.gate"))) return;
        if (args.length == 0) {
            if (!new MedievalFactions().USE_NEW_LANGUAGE_FILE) {
                player.sendMessage(translate("&b" + getText("SubCommands")));
                player.sendMessage(translate("&b" + getText("HelpGateCreate")));
                player.sendMessage(translate("&b" + getText("HelpGateName")));
                player.sendMessage(translate("&b" + getText("HelpGateList")));
                player.sendMessage(translate("&b" + getText("HelpGateRemove")));
                player.sendMessage(translate("&b" + getText("HelpGateCancel")));
            } else {
                playerService.sendListMessage(player, messageService.getLanguage().getStringList("GateHelp"));
            }
            return;
        }
        if (safeEquals(args[0], "cancel", playerService.getMessageType(getText("CmdGateCancel"), messageService.getLanguage().getString("Alias.CmdGateCancel")))) {
            // Cancel Logic
            if (ephemeralData.getCreatingGatePlayers().remove(player.getUniqueId()) != null) {
                playerService.sendMessageType(player, "&c" + getText("CreatingGateCancelled"), "CreatingGateCancelled", false);
                return;
            }
        }
        if (safeEquals(args[0], "create", playerService.getMessageType(getText("CmdGateCreate"), messageService.getLanguage().getString("Alias.CmdGateCreate")))) {
            // Create Logic
            if (ephemeralData.getCreatingGatePlayers().containsKey(player.getUniqueId())) {
                playerService.sendMessageType(player, "&c" + getText("AlertAlreadyCreatingGate"), "AlertAlreadyCreatingGate", false);
                return;
            }
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                playerService.sendMessageType(player, "&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand"), "AlertMustBeOwnerOrOfficerToUseCommand", false);
                return;
            }
            final String gateName;
            if (args.length > 1) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gateName = String.join(" ", arguments);
            } else {
                gateName = playerService.getMessageType("Unnamed Gate", messageService.getLanguage().getString("UnnamedGate"));
            }
            startCreatingGate(player, gateName);
            playerService.sendMessageType(player, "&b" + getText("CreatingGateClickWithHoe"), "CreatingGateClickWithHoe", false);
            return;
        }
        if (safeEquals(args[0], "list", playerService.getMessageType(getText("CmdGateList"), messageService.getLanguage().getString("Alias.CmdGateList")))) {
            // List logic
            if (faction.getGates().size() > 0) {
                playerService.sendMessageType(player, "&bFaction Gates", "FactionGate", false);
                for (Gate gate : faction.getGates()) {
                    playerService.sendMessageType(player, "&b" + String.format("%s: %s", gate.getName(), gate.coordsToString()),
                            Objects.requireNonNull(messageService.getLanguage().getString("GateLocation"))
                                    .replace("#name#", gate.getName())
                                    .replace("#location#", gate.coordsToString()), true);
                }
            } else {
                playerService.sendMessageType(player, "&c" + getText("AlertNoGatesDefined"), "AlertNoGatesDefined", false);
            }
            return;
        }
        final boolean remove = safeEquals(args[0], "remove", playerService.getMessageType(getText("CmdGateRemove"), messageService.getLanguage().getString("Alias.CmdGateRemove")));
        final boolean rename = safeEquals(args[0], "name", playerService.getMessageType(getText("CmdGateName"), messageService.getLanguage().getString("Alias.CmdGateName")));
        if (rename || remove) {
            final Block targetBlock = player.getTargetBlock(null, 16);
            if (targetBlock.getType().equals(Material.AIR)) {
                playerService.sendMessageType(player, "&c" + getText("NoBlockDetectedToCheckForGate")
                        , "NoBlockDetectedToCheckForGate", false);
                return;
            }
            if (!persistentData.isGateBlock(targetBlock)) {
                playerService.sendMessageType(player, "&c" + getText("TargetBlockNotPartOfGate")
                        , "TargetBlockNotPartOfGate", false);
                return;
            }
            final Gate gate = persistentData.getGate(targetBlock);
            if (gate == null) {
                playerService.sendMessageType(player, "&c" + getText("TargetBlockNotPartOfGate")
                        , "TargetBlockNotPartOfGate", false);
                return;
            }
            final Faction gateFaction = persistentData.getGateFaction(gate);
            if (gateFaction == null) {
                playerService.sendMessageType(player, "&c" + getText("ErrorCouldNotFindGatesFaction", gate.getName())
                        , Objects.requireNonNull(messageService.getLanguage().getString("ErrorCouldNotFindGatesFaction"))
                                .replace("#name#", gate.getName())
                        , true);
                return;
            }
            if (!gateFaction.isOfficer(player.getUniqueId()) && !gateFaction.isOwner(player.getUniqueId())) {
                playerService.sendMessageType(player, "&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand"), "AlertMustBeOwnerOrOfficerToUseCommand", false);
                return;
            }
            if (remove) {
                gateFaction.removeGate(gate);
                playerService.sendMessageType(player, "&b" + getText("RemovedGate", gate.getName())
                        , Objects.requireNonNull(messageService.getLanguage().getString("RemovedGate"))
                                .replace("#name#", gate.getName())
                        , true);
            }
            if (rename) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gate.setName(String.join(" ", arguments));
                playerService.sendMessageType(player, "&b" + getText("AlertChangedGateName", gate.getName())
                        , Objects.requireNonNull(messageService.getLanguage().getString("AlertChangedGateName"))
                                .replace("#name#", gate.getName())
                        , true);
            }
        }
    }

    /**
     * Method to execute the command.
     *
     * @param sender who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(CommandSender sender, String[] args, String key) {

    }

    private void startCreatingGate(Player player, String name) {
        ephemeralData.getCreatingGatePlayers().putIfAbsent(player.getUniqueId(), new Gate(name, medievalFactions, configService));
    }
}