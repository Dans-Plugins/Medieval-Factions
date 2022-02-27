/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.Gate;

/**
 * @author Callum Johnson
 */
public class GateCommand extends SubCommand {

    public GateCommand() {
        super(new String[]{
                "gate", "gt", LOCALE_PREFIX + "CmdGate"
        }, true, true);
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
            player.sendMessage(translate("&b" + getText("SubCommands")));
            player.sendMessage(translate("&b" + getText("HelpGateCreate")));
            player.sendMessage(translate("&b" + getText("HelpGateName")));
            player.sendMessage(translate("&b" + getText("HelpGateList")));
            player.sendMessage(translate("&b" + getText("HelpGateRemove")));
            player.sendMessage(translate("&b" + getText("HelpGateCancel")));
            return;
        }
        if (safeEquals(args[0], "cancel", getText("CmdGateCancel"))) {
            // Cancel Logic
            if (ephemeral.getCreatingGatePlayers().remove(player.getUniqueId()) != null) {
                player.sendMessage(translate("&c" + getText("CreatingGateCancelled")));
                return;
            }
        }
        if (safeEquals(args[0], "create", getText("CmdGateCreate"))) {
            // Create Logic
            if (ephemeral.getCreatingGatePlayers().containsKey(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertAlreadyCreatingGate")));
                return;
            }
            if (!faction.isOfficer(player.getUniqueId()) && !faction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                return;
            }
            final String gateName;
            if (args.length > 1) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gateName = String.join(" ", arguments);
            } else {
                gateName = "Unnamed Gate";
            }
            startCreatingGate(player, gateName);
            player.sendMessage(translate("&b" + getText("CreatingGateClickWithHoe")));
            return;
        }
        if (safeEquals(args[0], "list", getText("CmdGateList"))) {
            // List logic
            if (faction.getGates().size() > 0) {
                player.sendMessage(translate("&bFaction Gates"));
                for (Gate gate : faction.getGates()) {
                    player.sendMessage(translate("&b" + String.format("%s: %s", gate.getName(), gate.coordsToString())));
                }
            } else {
                player.sendMessage(translate("&c" + getText("AlertNoGatesDefined")));
            }
            return;
        }
        final boolean remove = safeEquals(args[0], "remove", getText("CmdGateRemove"));
        final boolean rename = safeEquals(args[0], "name", getText("CmdGateName"));
        if (rename || remove) {
            final Block targetBlock = player.getTargetBlock(null, 16);
            if (targetBlock.getType().equals(Material.AIR)) {
                player.sendMessage(translate("&c" + getText("NoBlockDetectedToCheckForGate")));
                return;
            }
            if (!data.isGateBlock(targetBlock)) {
                player.sendMessage(translate("&c" + getText("TargetBlockNotPartOfGate")));
                return;
            }
            final Gate gate = PersistentData.getInstance().getGate(targetBlock);
            if (gate == null) {
                player.sendMessage(translate("&c" + getText("TargetBlockNotPartOfGate")));
                return;
            }
            final Faction gateFaction = PersistentData.getInstance().getGateFaction(gate);
            if (gateFaction == null) {
                player.sendMessage(translate("&c" + getText("ErrorCouldNotFindGatesFaction", gate.getName())));
                return;
            }
            if (!gateFaction.isOfficer(player.getUniqueId()) && !gateFaction.isOwner(player.getUniqueId())) {
                player.sendMessage(translate("&c" + getText("AlertMustBeOwnerOrOfficerToUseCommand")));
                return;
            }
            if (remove) {
                gateFaction.removeGate(gate);
                player.sendMessage(translate("&b" + getText("RemovedGate", gate.getName())));
            }
            if (rename) {
                String[] arguments = new String[args.length - 1];
                System.arraycopy(args, 1, arguments, 0, arguments.length);
                gate.setName(String.join(" ", arguments));
                player.sendMessage(translate("&b" + getText("AlertChangedGateName", gate.getName())));
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
        ephemeral.getCreatingGatePlayers().putIfAbsent(player.getUniqueId(), new Gate(name));
    }
}