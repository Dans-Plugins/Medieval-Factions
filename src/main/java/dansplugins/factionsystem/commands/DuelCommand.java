/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.objects.domain.Duel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static dansplugins.factionsystem.objects.domain.Duel.DuelState;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
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
}