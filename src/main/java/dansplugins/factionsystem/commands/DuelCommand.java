package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.objects.Duel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dansplugins.factionsystem.objects.Duel.DuelState;

public class DuelCommand extends SubCommand {

    public DuelCommand() {
        super(new String[]{"dl", "duel", LOCALE_PREFIX + "CmdDuel"}, true);
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
        final String permission = "mf.duel";
        if (!(checkPermissions(player, permission))) return;
        if (args.length <= 0) {
            sendHelp(player);
            return;
        }
        if (safeEquals(false, args[0], getText("CmdDuelChallenge"), "challenge")) {
            if (!(args.length >= 2)) {
                sendHelp(player);
                return;
            }
            if (player.getName().equals(args[1])) {
                player.sendMessage(translate("&c" + getText("CannotDuelSelf")));
                return;
            }
            if (isDuelling(player)) {
                player.sendMessage(translate("&c" + getText("AlertAlreadyDuelingSomeone")));
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                return;
            }
            if (isDuelling(target)) {
                player.sendMessage(translate("&c" + getText("PlayerAlreadyDueling", target.getName())));
                return;
            }
            int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
            if (args.length == 3) timeLimit = getIntSafe(args[2], 120);
            inviteDuel(player, target, timeLimit);
            player.sendMessage(translate("&b" + getText("AlertChallengeIssued", target.getName())));
        } else if (safeEquals(false, args[0], getText("CmdDuelAccept"), "accept")) {
            if (isDuelling(player)) {
                player.sendMessage(translate("&c" + getText("AlertAlreadyDuelingSomeone")));
                return;
            }
            final Duel duel;
            final String notChallenged, alreadyDueling;
            if (args.length >= 2) {
                final Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    player.sendMessage(translate("&c" + getText("PlayerNotFound")));
                    return;
                }
                duel = ephemeral.getDuel(player, target);
                notChallenged = getText("AlertNotBeenChallengedByPlayer", target.getName());
                alreadyDueling = getText("AlertAlreadyDuelingPlayer", target.getName());
            } else {
                duel = getDuel(player);
                notChallenged = getText("AlertNotBeenChallenged");
                alreadyDueling = getText("AlertAlreadyDueling");
            }
            if (duel == null) {
                player.sendMessage(translate("&c" + notChallenged));
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                player.sendMessage(translate("&c" + alreadyDueling));
                return;
            }
            if (!(duel.isChallenged(player))) {
                player.sendMessage(translate("&c" + notChallenged));
                return;
            }
            duel.acceptDuel();
        } else if (safeEquals(false, args[0], getText("CmdDuelCancel"), "cancel")) {
            if (!isDuelling(player)) {
                player.sendMessage(translate("&c" + getText("AlertNoPendingChallenges")));
                return;
            }
            final Duel duel = getDuel(player);
            if (duel == null) {
                player.sendMessage(translate("&c" + getText("AlertNoPendingChallenges")));
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                player.sendMessage(translate("&c" + getText("CannotCancelActiveDuel")));
                return;
            }
            ephemeral.getDuelingPlayers().remove(duel);
            player.sendMessage(translate("&b" + getText("DuelChallengeCancelled")));
        } else {
            sendHelp(player);
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

    private void sendHelp(CommandSender sender) {
        sender.sendMessage(translate("&b" + getText("SubCommands")));
        sender.sendMessage(translate("&b" + getText("HelpDuelChallenge")));
        sender.sendMessage(translate("&b" + getText("HelpDuelAccept")));
        sender.sendMessage(translate("&b" + getText("HelpDuelCancel")));
    }

	private Duel getDuel(Player player) {
		return ephemeral.getDuelingPlayers().stream()
				.filter(duel -> duel.isChallenged(player) || duel.isChallenger(player))
				.findFirst().orElse(null);
	}

	private boolean isDuelling(Player player) {
		return ephemeral.getDuelingPlayers().stream()
				.anyMatch(duel -> duel.hasPlayer(player) && duel.getStatus().equals(DuelState.DUELLING));
	}

	private void inviteDuel(Player player, Player target, int limit) {
		target.sendMessage(translate("&b" + getText("AlertChallengedToDuelPlusHowTo", player.getName())));
		ephemeral.getDuelingPlayers().add(new Duel(player, target, limit));
	}


	@Deprecated
    public void handleDuel(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.duel")) {
                if (args.length > 1) {
                    if (args[1].equalsIgnoreCase("challenge") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDuelChallenge"))) {
                        if (args.length > 2) {
                            if (args[2].equalsIgnoreCase(player.getName())) {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDuelSelf"));
                                return;
                            }
                            if (isDuelling(player)) {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDuelingSomeone"));
                                return;
                            }
                            Player target = Bukkit.getServer().getPlayer(args[2]);
                            if (target != null) {
                                if (!isDuelling(target)) {
                                    int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
                                    if (args.length == 4) {
                                        timeLimit = Integer.parseInt(args[3]);
                                    }
                                    inviteDuel(player, target, timeLimit);
                                    player.sendMessage(ChatColor.AQUA + String.format(LocaleManager.getInstance().getText("AlertChallengeIssued"), target.getName()));
                                    return;
                                } else {
                                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PlayerAlreadyDueling"), target.getName()));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerNotFound"));
                                return;
                            }
                        }
                    } else if (args[1].equalsIgnoreCase("accept") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDuelAccept"))) {
                        if (isDuelling(player)) {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDuelingSomeone"));
                            return;
                        }
                        // If a name is specified to accept the challenge from, look for that specific name.
                        if (args.length > 2) {
                            Player challenger = Bukkit.getServer().getPlayer(args[2]);
                            Duel duel = EphemeralData.getInstance().getDuel(challenger, player);
                            if (duel != null) {
                                if (duel.getStatus().equals(Duel.DuelState.DUELLING)) {
                                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertAlreadyDuelingPlayer"), args[2]));
                                    return;
                                }
                                if (duel.isChallenged(player)) {
                                    duel.acceptDuel();
                                } else {
                                    player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertNotBeenChallengedByPlayer"), args[2]));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("AlertNotBeenChallengedByPlayer"), args[2]));
                                return;
                            }
                        } else {
                            Duel duel = getDuel(player);
                            if (duel != null) {
                                if (duel.getStatus().equals(Duel.DuelState.DUELLING)) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDueling"));
                                    return;
                                }
                                if (duel.isChallenged(player)) {
                                    duel.acceptDuel();
                                } else {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallenged"));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallenged"));
                                return;
                            }
                        }
                    } else if (args[1].equalsIgnoreCase("cancel") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdDuelCancel"))) {
                        if (isDuelling(player)) {
                            Duel duel = getDuel(player);
                            if (duel != null) {
                                if (duel.getStatus().equals(Duel.DuelState.DUELLING)) {
                                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotCancelActiveDuel"));
                                    return;
                                } else {
                                    EphemeralData.getInstance().getDuelingPlayers().remove(duel);
                                    player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("DuelChallengeCancelled"));
                                    return;
                                }
                            } else {
                                player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertNoPendingChallenges"));
                                return;
                            }
                        } else {
                            player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertNoPendingChallenges"));
                            return;
                        }

                    } else {
                        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
                        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelChallenge"));
                        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelAccept"));
                        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelCancel"));
                    }
                } else {
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelChallenge"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelAccept"));
                    sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelCancel"));
                }
            }
        }
    }

}
