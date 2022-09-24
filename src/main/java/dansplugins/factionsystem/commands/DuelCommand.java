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
import dansplugins.factionsystem.objects.domain.Duel;
import dansplugins.factionsystem.objects.domain.Duel.DuelState;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class DuelCommand extends SubCommand {
    private final MedievalFactions medievalFactions;

    public DuelCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions, PlayerService playerService, MessageService messageService) {
        super(new String[]{"dl", "duel", LOCALE_PREFIX + "CmdDuel"}, true, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.duel";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            sendHelp(player);
            return;
        }
        if (safeEquals(args[0], playerService.decideWhichMessageToUse(getText("CmdDuelChallenge"), messageService.getLanguage().getString("Alias.  CmdDuelChallenge")), "challenge")) {
            if (!(args.length >= 2)) {
                sendHelp(player);
                return;
            }
            if (player.getName().equals(args[1])) {
                playerService.sendMessage(player, "&c" + getText("CannotDuelSelf"), "CannotDuelSelf", false);
                return;
            }
            if (isDuelling(player)) {
                playerService.sendMessage(player, "&c" + getText("AlertAlreadyDuelingSomeone"), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[1]), true);
                return;
            }
            if (isDuelling(target)) {
                playerService.sendMessage(player, "&c" + getText("PlayerAlreadyDueling", target.getName()), Objects.requireNonNull(messageService.getLanguage().getString("PlayerAlreadyDueling")).replace("#name#", args[1]), true);
                return;
            }
            int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
            if (args.length == 3) {
                timeLimit = getIntSafe(args[2], 120);
            }
            inviteDuel(player, target, timeLimit);
            playerService.sendMessage(player, "&b" + getText("AlertChallengeIssued", target.getName()), Objects.requireNonNull(messageService.getLanguage().getString("AlertChallengeIssued")).replace("#name#", target.getName()), true);
        } else if (safeEquals(args[0], playerService.decideWhichMessageToUse(getText("CmdDuelAccept"), messageService.getLanguage().getString("Alias.CmdDuelAccept")), "accept")) {
            if (isDuelling(player)) {
                playerService.sendMessage(player, "&c" + getText("AlertAlreadyDuelingSomeone"), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            final Duel duel;
            final String notChallenged, alreadyDueling, notChallenged2, alreadyDueling2;
            if (args.length >= 2) {
                final Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    playerService.sendMessage(player, "&c" + getText("PlayerNotFound"), Objects.requireNonNull(messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[1]), true);
                    return;
                }
                duel = ephemeralData.getDuel(player, target);
                notChallenged = getText("AlertNotBeenChallengedByPlayer", target.getName());
                notChallenged2 = Objects.requireNonNull(messageService.getLanguage().getString("AlertNotBeenChallengedByPlayer")).replace("#name#", target.getName());
                alreadyDueling = getText("AlertAlreadyDuelingPlayer", target.getName());
                alreadyDueling2 = Objects.requireNonNull(messageService.getLanguage().getString("AlertAlreadyDuelingPlayer")).replace("#name#", target.getName());
            } else {
                duel = getDuel(player);
                notChallenged = getText("AlertNotBeenChallenged");
                alreadyDueling = getText("AlertAlreadyDueling");
                notChallenged2 = messageService.getLanguage().getString("AlertNotBeenChallenged");
                alreadyDueling2 = messageService.getLanguage().getString("AlertAlreadyDueling");
            }
            if (duel == null) {
                playerService.sendMessage(player, "&c" + notChallenged, notChallenged2, true);
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                playerService.sendMessage(player, "&c" + alreadyDueling, alreadyDueling2, true);
                return;
            }
            if (!(duel.isChallenged(player))) {
                playerService.sendMessage(player, "&c" + notChallenged, notChallenged2, true);
                return;
            }
            duel.acceptDuel();
        } else if (safeEquals(args[0], playerService.decideWhichMessageToUse(getText("CmdDuelCancel"), messageService.getLanguage().getString("Alias.CmdDuelCancel")), "cancel")) {
            if (!isDuelling(player)) {
                playerService.sendMessage(player, "&c" + getText("AlertNoPendingChallenges"), "AlertNoPendingChallenges", false);
                return;
            }
            final Duel duel = getDuel(player);
            if (duel == null) {
                playerService.sendMessage(player, "&c" + getText("AlertNoPendingChallenges"), "AlertNoPendingChallenges", false);
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                playerService.sendMessage(player, "c" + getText("CannotCancelActiveDuel"), "CannotCancelActiveDuel", false);
                return;
            }
            ephemeralData.getDuelingPlayers().remove(duel);
            playerService.sendMessage(player, "&b" + getText("DuelChallengeCancelled"), "DuelChallengeCancelled", false);
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
        if (!configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage("&b" + getText("SubCommands"));
            sender.sendMessage("&b" + getText("HelpDuelChallenge"));
            sender.sendMessage("&b" + getText("HelpDuelAccept"));
            sender.sendMessage("&b" + getText("HelpDuelCancel"));
        } else {
            playerService.sendMultipleMessages(sender, messageService.getLanguage().getStringList("DuelHelp"));
        }
    }

    private Duel getDuel(Player player) {
        return ephemeralData.getDuelingPlayers().stream().filter(duel -> duel.isChallenged(player) || duel.isChallenger(player)).findFirst().orElse(null);
    }

    private boolean isDuelling(Player player) {
        return ephemeralData.getDuelingPlayers().stream().anyMatch(duel -> duel.hasPlayer(player) && duel.getStatus().equals(DuelState.DUELLING));
    }

    private void inviteDuel(Player player, Player target, int limit) {
        playerService.sendMessage(target, "&a" + getText("AlertChallengedToDuelPlusHowTo", player.getName()),
                Objects.requireNonNull(messageService.getLanguage().getString("AlertChallengedToDuelPlusHowTo"))
                        .replace("#name#", player.getName()), true);
        ephemeralData.getDuelingPlayers().add(new Duel(medievalFactions, ephemeralData, player, target, limit));
    }
}