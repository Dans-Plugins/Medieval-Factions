package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.Gate;
import org.bukkit.ChatColor;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;

public class GateCommand extends SubCommand {

	public GateCommand() {
		super(new String[] {
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
			player.sendMessage(translate("&c" + getText("SubCommands")));
			player.sendMessage(translate("&c" + getText("HelpGateCreate")));
			player.sendMessage(translate("&c" + getText("HelpGateName")));
			player.sendMessage(translate("&c" + getText("HelpGateList")));
			player.sendMessage(translate("&c" + getText("HelpGateRemove")));
			player.sendMessage(translate("&c" + getText("HelpGateCancel")));
			return;
		}
		if (safeEquals(false, args[0], "cancel", getText("CmdGateCancel"))) {
			// Cancel Logic
			if (ephemeral.getCreatingGatePlayers().remove(player.getUniqueId()) != null) {
				player.sendMessage(translate("&c" + getText("CreatingGateCancelled")));
				return;
			}
		}
		if (safeEquals(false, args[0], "create", getText("CmdGateCreate"))) {
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
		if (safeEquals(false, args[0], "list", getText("CmdGateList"))) {
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
		final boolean remove = safeEquals(false, args[0], "remove", getText("CmdGateRemove"));
		final boolean rename = safeEquals(false, args[0], "name", getText("CmdGateName"));
		if (rename || remove) {
			final Block targettedBlock = player.getTargetBlock(null, 16);
			if (targettedBlock.getType().equals(Material.AIR)) {
				player.sendMessage(translate("&c" + getText("NoBlockDetectedToCheckForGate")));
				return;
			}
			if (!data.isGateBlock(targettedBlock)) {
				player.sendMessage(translate("&c" + getText("TargetBlockNotPartOfGate")));
				return;
			}
			final Gate gate = getGate(targettedBlock, data.getFactions());
			if (gate == null) {
				player.sendMessage(translate("&c" + getText("TargetBlockNotPartOfGate")));
				return;
			}
			final Faction gateFaction = getGateFaction(gate, data.getFactions());
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

	private Gate getGate(Block targetBlock, ArrayList<Faction> factions) {
		return factions.stream().flatMap(faction -> faction.getGates().stream())
				.filter(gate -> gate.hasBlock(targetBlock)).findFirst().orElse(null);
	}

	private Faction getGateFaction(Gate gate, ArrayList<Faction> factions) {
		return factions.stream()
				.filter(faction -> faction.getGates().contains(gate)).findFirst().orElse(null);
	}

	@Deprecated
    public void handleGate(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.gate")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("cancel") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateCancel"))) {
                        if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId())) {
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CreatingGateCancelled"));
                            return;
                        }
                    }
                    if (args[1].equalsIgnoreCase("create") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateCreate"))) {
                        if (EphemeralData.getInstance().getCreatingGatePlayers().containsKey(player.getUniqueId())) {
                            EphemeralData.getInstance().getCreatingGatePlayers().remove(player.getUniqueId());
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyCreatingGate"));
                            return;
                        } else {
                            Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                            if (faction != null) {
                                if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId())) {
                                    String gateName = "Unnamed Gate";
                                    if (args.length > 2) {
                                        gateName = createStringFromArgIndexOnwards(2, args);
                                    }
                                    startCreatingGate(player, gateName);
                                    //TODO: Config setting for magic gate tool.
                                    player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("CreatingGateClickWithHoe"));
                                    return;
                                } else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
                                    return;
                                }
                            }
                        }
                    }
                    if (args[1].equalsIgnoreCase("list") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateList"))) {
                        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
                        if (faction != null) {
                            if (faction.getGates().size() > 0) {
                                player.sendMessage(ChatColor.AQUA + "Faction Gates");
                                for (Gate gate : faction.getGates()) {
                                    player.sendMessage(ChatColor.AQUA + String.format("%s: %s", gate.getName(), gate.coordsToString()));
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNoGatesDefined"));
                                return;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NotAMemberOfAnyFaction"));
                            return;
                        }
                    }
                    if (args[1].equalsIgnoreCase("remove") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateRemove"))) {
                        if (player.getTargetBlock(null, 16) != null) {
                            if (PersistentData.getInstance().isGateBlock(player.getTargetBlock(null, 16))) {
                                Gate gate = getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
                                Faction faction = getGateFaction(gate, PersistentData.getInstance().getFactions());
                                if (faction != null) {
                                    if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId())) {
                                        faction.removeGate(gate);
                                        player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("RemovedGate"), gate.getName()));
                                        return;
                                    } else {
                                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToUseCommand"));
                                        return;
                                    }
                                } else {
                                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("ErrorCouldNotFindGatesFaction"), gate.getName()));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TargetBlockNotPartOfGate"));
                                return;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NoBlockDetectedToCheckForGate"));
                            return;
                        }
                    }
                    if (args[1].equalsIgnoreCase("name") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdGateName"))) {
                        if (player.getTargetBlock(null, 16) != null) {
                            if (PersistentData.getInstance().isGateBlock(player.getTargetBlock(null, 16))) {
                                Gate gate = getGate(player.getTargetBlock(null, 16), PersistentData.getInstance().getFactions());
                                if (args.length > 2) {
                                    Faction faction = getGateFaction(gate, PersistentData.getInstance().getFactions());
                                    if (faction != null) {
                                        if (faction.isOfficer(player.getUniqueId()) || faction.isOwner(player.getUniqueId())) {
                                            String name = createStringFromArgIndexOnwards(2, args);
                                            gate.setName(name);
                                            player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertChangedGateName"), gate.getName()));
                                            return;
                                        } else {
                                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerOrOfficerToCreateGate"));
                                            return;
                                        }
                                    } else {
                                        player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("ErrorCouldNotFindGatesFaction"), gate.getName()));
                                        return;
                                    }
                                } else {
                                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertGate"), gate.getName()));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("TargetBlockNotPartOfGate"));
                                return;
                            }
                        } else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("NoBlockDetectedToCheckForGate"));
                            return;
                        }
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateCreate"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateName"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateList"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateRemove"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpGateCancel"));
                    return;
                }
            } else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.gate"));
            }

        }
    }

    private String createStringFromArgIndexOnwards(int index, String[] args) {
        StringBuilder name = new StringBuilder();
        for (int i = index; i < args.length; i++) {
            name.append(args[i]);
            if (!(i == args.length - 1)) {
                name.append(" ");
            }
        }
        return name.toString();
    }

}
