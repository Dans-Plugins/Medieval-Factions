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

    public DuelCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, MedievalFactions medievalFactions) {
        super(new String[]{"dl", "duel", LOCALE_PREFIX + "CmdDuel"}, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
        if (safeEquals(args[0], PlayerService.getMessageType(getText("CmdDuelChallenge"), MessageService.getLanguage().getString("Alias.  CmdDuelChallenge")), "challenge")) {
            if (!(args.length >= 2)) {
                sendHelp(player);
                return;
            }
            if (player.getName().equals(args[1])) {
                PlayerService.sendMessageType(player, translate("&c" + getText("CannotDuelSelf")), "CannotDuelSelf", false);
                return;
            }
            if (isDuelling(player)) {
                PlayerService.sendMessageType(player, translate("&c" + getText("AlertAlreadyDuelingSomeone")), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                PlayerService.sendMessageType(player, translate("&c" + getText("PlayerNotFound")), Objects.requireNonNull(MessageService.getLanguage().getString("PlayerNotFound")).replaceAll("#name#", args[1]), true);
                return;
            }
            if (isDuelling(target)) {
                PlayerService.sendMessageType(player, translate("&c" + getText("PlayerAlreadyDueling", target.getName())), Objects.requireNonNull(MessageService.getLanguage().getString("PlayerAlreadyDueling")).replaceAll("#name#", args[1]), true);
                return;
            }
            int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
            if (args.length == 3) {
                timeLimit = getIntSafe(args[2], 120);
            }
            inviteDuel(player, target, timeLimit);
            PlayerService.sendMessageType(player, ("&b" + getText("AlertChallengeIssued", target.getName())), Objects.requireNonNull(MessageService.getLanguage().getString("AlertChallengeIssued")).replaceAll("#name#", target.getName()), true);
        } else if (safeEquals(args[0], PlayerService.getMessageType(getText("CmdDuelAccept"), MessageService.getLanguage().getString("Alias.CmdDuelAccept")), "accept")) {
            if (isDuelling(player)) {
                PlayerService.sendMessageType(player, ("&c" + getText("AlertAlreadyDuelingSomeone")), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            final Duel duel;
            final String notChallenged, alreadyDueling, notChallenged2, alreadyDueling2;
            if (args.length >= 2) {
                final Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    PlayerService.sendMessageType(player, ("&c" + getText("PlayerNotFound")), Objects.requireNonNull(MessageService.getLanguage().getString("PlayerNotFound")).replaceAll("#name#", args[1]), true);
                    return;
                }
                duel = ephemeralData.getDuel(player, target);
                notChallenged = getText("AlertNotBeenChallengedByPlayer", target.getName());
                notChallenged2 = Objects.requireNonNull(MessageService.getLanguage().getString("AlertNotBeenChallengedByPlayer")).replaceAll("#name#", target.getName());
                alreadyDueling = getText("AlertAlreadyDuelingPlayer", target.getName());
                alreadyDueling2 = Objects.requireNonNull(MessageService.getLanguage().getString("AlertAlreadyDuelingPlayer")).replaceAll("#name#", target.getName());
            } else {
                duel = getDuel(player);
                notChallenged = getText("AlertNotBeenChallenged");
                alreadyDueling = getText("AlertAlreadyDueling");
                notChallenged2 = MessageService.getLanguage().getString("AlertNotBeenChallenged");
                alreadyDueling2 = MessageService.getLanguage().getString("AlertAlreadyDueling");
            }
            if (duel == null) {
                PlayerService.sendMessageType(player, ("&c" + notChallenged), notChallenged2, true);
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                PlayerService.sendMessageType(player, "&c" + alreadyDueling, alreadyDueling2, true);
                return;
            }
            if (!(duel.isChallenged(player))) {
                PlayerService.sendMessageType(player, ("&c" + notChallenged), notChallenged2, true);
                return;
            }
            duel.acceptDuel();
        } else if (safeEquals(args[0], PlayerService.getMessageType(getText("CmdDuelCancel"), MessageService.getLanguage().getString("Alias.CmdDuelCancel")), "cancel")) {
            if (!isDuelling(player)) {
                PlayerService.sendMessageType(player, "&c" + getText("AlertNoPendingChallenges"), "AlertNoPendingChallenges", false);
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
            ephemeralData.getDuelingPlayers().remove(duel);
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
        return ephemeralData.getDuelingPlayers().stream().filter(duel -> duel.isChallenged(player) || duel.isChallenger(player)).findFirst().orElse(null);
    }

    private boolean isDuelling(Player player) {
        return ephemeralData.getDuelingPlayers().stream().anyMatch(duel -> duel.hasPlayer(player) && duel.getStatus().equals(DuelState.DUELLING));
    }

    private void inviteDuel(Player player, Player target, int limit) {
        target.sendMessage(translate("&b" + getText("AlertChallengedToDuelPlusHowTo", player.getName())));
        ephemeralData.getDuelingPlayers().add(new Duel(medievalFactions, ephemeralData, player, target, limit));
    }
}