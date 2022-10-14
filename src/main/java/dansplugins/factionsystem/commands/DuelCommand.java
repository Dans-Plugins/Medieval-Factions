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
import dansplugins.factionsystem.utils.TabCompleteTools;
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
        super(new String[]{
            "duel", "dl", LOCALE_PREFIX + "CmdDuel"
        }, true, ["mf.duel"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        if (args.length == 0) {
            sendHelp(player);
            return;
        }
        if (this.safeEquals(args[0], this.playerService.decideWhichMessageToUse(this.getText("CmdDuelChallenge"), this.messageService.getLanguage().getString("Alias.  CmdDuelChallenge")), "challenge")) {
            if (!(args.length >= 2)) {
                this.sendHelp(player);
                return;
            }
            if (player.getName().equals(args[1])) {
                this.playerService.sendMessage(player, "&c" + this.getText("CannotDuelSelf"), "CannotDuelSelf", false);
                return;
            }
            if (isDuelling(player)) {
                pthis.layerService.sendMessage(player, "&c" + this.getText("AlertAlreadyDuelingSomeone"), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            Player target = Bukkit.getPlayer(args[1]);
            if (target == null) {
                this.playerService.sendMessage(player, "&c" + this.getText("PlayerNotFound"), Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[1]), true);
                return;
            }
            if (isDuelling(target)) {
                this.playerService.sendMessage(player, "&c" + this.getText("PlayerAlreadyDueling", target.getName()), Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerAlreadyDueling")).replace("#name#", args[1]), true);
                return;
            }
            int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
            if (args.length == 3) {
                timeLimit = this.getIntSafe(args[2], 120);
            }
            inviteDuel(player, target, timeLimit);
            this.playerService.sendMessage(player, "&b" + this.getText("AlertChallengeIssued", target.getName()), Objects.requireNonNull(this.messageService.getLanguage().getString("AlertChallengeIssued")).replace("#name#", target.getName()), true);
        } else if (this.safeEquals(args[0], this.playerService.decideWhichMessageToUse(this.getText("CmdDuelAccept"), this.messageService.getLanguage().getString("Alias.CmdDuelAccept")), "accept")) {
            if (isDuelling(player)) {
                this.playerService.sendMessage(player, "&c" + this.getText("AlertAlreadyDuelingSomeone"), "AlertAlreadyDuelingSomeone", false);
                return;
            }
            final Duel duel;
            final String notChallenged, alreadyDueling, notChallenged2, alreadyDueling2;
            if (args.length >= 2) {
                final Player target = Bukkit.getPlayer(args[2]);
                if (target == null) {
                    this.playerService.sendMessage(player, "&c" + this.getText("PlayerNotFound"), Objects.requireNonNull(this.messageService.getLanguage().getString("PlayerNotFound")).replace("#name#", args[1]), true);
                    return;
                }
                duel = this.ephemeralData.getDuel(player, target);
                notChallenged = this.getText("AlertNotBeenChallengedByPlayer", target.getName());
                notChallenged2 = Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNotBeenChallengedByPlayer")).replace("#name#", target.getName());
                alreadyDueling = this.getText("AlertAlreadyDuelingPlayer", target.getName());
                alreadyDueling2 = Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAlreadyDuelingPlayer")).replace("#name#", target.getName());
            } else {
                duel = getDuel(player);
                notChallenged = this.getText("AlertNotBeenChallenged");
                alreadyDueling = this.getText("AlertAlreadyDueling");
                notChallenged2 = this.messageService.getLanguage().getString("AlertNotBeenChallenged");
                alreadyDueling2 = this.messageService.getLanguage().getString("AlertAlreadyDueling");
            }
            if (duel == null) {
                this.playerService.sendMessage(player, "&c" + notChallenged, notChallenged2, true);
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                this.playerService.sendMessage(player, "&c" + alreadyDueling, alreadyDueling2, true);
                return;
            }
            if (!(duel.isChallenged(player))) {
                this.playerService.sendMessage(player, "&c" + notChallenged, notChallenged2, true);
                return;
            }
            duel.acceptDuel();
        } else if (this.safeEquals(args[0], this.playerService.decideWhichMessageToUse(this.getText("CmdDuelCancel"), this.messageService.getLanguage().getString("Alias.CmdDuelCancel")), "cancel")) {
            if (!isDuelling(player)) {
                playerService.sendMessage(player, "&c" + getText("AlertNoPendingChallenges"), "AlertNoPendingChallenges", false);
                return;
            }
            final Duel duel = getDuel(player);
            if (duel == null) {
                this.playerService.sendMessage(player, "&c" + this.getText("AlertNoPendingChallenges"), "AlertNoPendingChallenges", false);
                return;
            }
            if (duel.getStatus().equals(DuelState.DUELLING)) {
                this.playerService.sendMessage(player, "c" + this.getText("CannotCancelActiveDuel"), "CannotCancelActiveDuel", false);
                return;
            }
            this.ephemeralData.getDuelingPlayers().remove(duel);
            this.playerService.sendMessage(player, "&b" + this.getText("DuelChallengeCancelled"), "DuelChallengeCancelled", false);
        } else {
            this.sendHelp(player);
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
        if (!this.configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage("&b" + this.getText("SubCommands"));
            sender.sendMessage("&b" + this.getText("HelpDuelChallenge"));
            sender.sendMessage("&b" + this.getText("HelpDuelAccept"));
            sender.sendMessage("&b" + this.getText("HelpDuelCancel"));
        } else {
            this.playerService.sendMultipleMessages(sender, this.messageService.getLanguage().getStringList("DuelHelp"));
        }
    }

    private Duel getDuel(Player player) {
        return this.ephemeralData.getDuelingPlayers().stream().filter(duel -> duel.isChallenged(player) || duel.isChallenger(player)).findFirst().orElse(null);
    }

    private boolean isDuelling(Player player) {
        return this.ephemeralData.getDuelingPlayers().stream().anyMatch(duel -> duel.hasPlayer(player) && duel.getStatus().equals(DuelState.DUELLING));
    }

    private void inviteDuel(Player player, Player target, int limit) {
        this.playerService.sendMessage(
            target, 
            "&a" + this.getText("AlertChallengedToDuelPlusHowTo", player.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertChallengedToDuelPlusHowTo")).replace("#name#", player.getName()), 
            true
        );
        this.ephemeralData.getDuelingPlayers().add(new Duel(this.medievalFactions, this.ephemeralData, player, target, limit));
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (args.length === 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "challenge", "accept", "cancel");
        } else if (args.length === 2) {
            if (args[0] === "challenge") return TabCompleteTools.allOnlinePlayersMatching(args[1]);
        }
        return null;
    }
}