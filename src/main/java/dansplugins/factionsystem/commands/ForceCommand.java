package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.StorageManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.lang.reflect.Method;
import java.util.*;

public class ForceCommand extends SubCommand {

    private final String[] commands = new String[]{
            "Save", "Load", "Peace", "Demote", "Join", "Kick", "Power", "Renounce", "Transfer", "RemoveVassal"
    };
    private final HashMap<List<String>, String> subMap = new HashMap<>();

    public ForceCommand() {
        super(new String[]{
                "Force", LOCALE_PREFIX + "CmdForce"
        }, false);
        // Register sub-commands.
        Arrays.stream(commands).forEach(command ->
                subMap.put(Arrays.asList(command, getText("CmdForce" + command)), "force" + command)
        );
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
        if (!(args.length <= 0)) {
            for (Map.Entry<List<String>, String> entry : subMap.entrySet()) {
                if (safeEquals(false, args[0], (String[]) entry.getKey().toArray())) {
                    try {
                        Method method = getClass().getMethod(entry.getValue(), CommandSender.class, String[].class);
                        method.invoke(sender, (Object[]) args);
                    } catch (ReflectiveOperationException ex) {
                        System.out.println("DEBUG: Failed to resolve method from '" + args[0] + "'!");
                    }
                    return;
                }
            }
        }
        sender.sendMessage(translate("&c" + getText("SubCommands")));
        Arrays.stream(commands).forEach(str -> sender.sendMessage(translate("&c" + getText("HelpForce" + str))));
    }

    private void forceSave(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.save", "mf.force.*", "mf.admin"))) return;
        sender.sendMessage(translate("&a" + getText("AlertForcedSave")));
        StorageManager.getInstance().save();
    }

    private void forceLoad(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.load", "mf.force.*", "mf.admin"))) return;
        sender.sendMessage(translate("&a" + LocaleManager.getInstance().getText("AlertForcedLoad")));
        StorageManager.getInstance().load();
        MedievalFactions.getInstance().reloadConfig();
    }

    private void forcePeace(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.peace", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 4)) {
            sender.sendMessage(translate("&c" + getText("UsageForcePeace")));
            return;
        }
        // get arguments designated by single quotes
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + getText("NoFactionsDesignatedSingleQuotesRequired")));
            return;
        }
        final Faction former = PersistentData.getInstance().getFaction(singleQuoteArgs.get(0));
        final Faction latter = PersistentData.getInstance().getFaction(singleQuoteArgs.get(1));
        if (former == null || latter == null) {
            sender.sendMessage(translate("&c" + getText("DesignatedFactionNotFound")));
            return;
        }
        if (former.isEnemy(latter.getName())) former.removeEnemy(latter.getName()); // Remove
        if (latter.isEnemy(former.getName())) latter.removeEnemy(former.getName()); // Remove
        // announce peace to all players on server.
        messageServer(translate(
                "&a" + former.getName() + getText("AlertNowAtPeaceWith", latter.getName() + "!")
        ));
    }

    private void forceDemote(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.demote", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 2)) {
            sender.sendMessage(translate("&c" + getText("UsageForceDemote")));
            return;
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[1]);
        if (playerUUID == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final Faction faction = getPlayerFaction(player);
        if (!faction.isOfficer(player.getUniqueId())) {
            sender.sendMessage(translate("&c" + getText("PlayerIsNotOfficerOfFaction")));
            return;
        }
        faction.removeOfficer(player.getUniqueId()); // Remove Officer.
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(translate("&b" + getText("AlertForcedDemotion")));
        }
        sender.sendMessage(translate("&a" + getText("SuccessOfficerRemoval")));
    }

    private void forceJoin(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.join", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 4)) {
            sender.sendMessage(translate("&c" + getText("UsageForceJoin")));
            return;
        }
        // get arguments designated by single quotes
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + getText("NotEnoughArgumentsDesignatedSingleQuotesRequired")));
            return;
        }
        final Faction faction = getFaction(singleQuoteArgs.get(1));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(singleQuoteArgs.get(0));
        if (playerUUID == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (data.isInFaction(playerUUID)) {
            sender.sendMessage(translate("&c" + getText("PlayerAlreadyInFaction")));
            return;
        }
        faction.addMember(playerUUID, data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        messageFaction(faction, translate("&a" + getText("HasJoined", player.getName(), faction.getName())));
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(translate("&b" + getText("AlertForcedToJoinFaction")));
        }
        sender.sendMessage(translate("&a" + getText("SuccessForceJoin")));
    }

    private void forceKick(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.kick", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 2)) {
            sender.sendMessage(translate("&c" + getText("UsageForceKick")));
            return;
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (playerUUID == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final Faction faction = getPlayerFaction(player);
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (faction.isOwner(playerUUID)) {
            sender.sendMessage(translate("&c" + getText("CannotForciblyKickOwner")));
            return;
        }
        faction.removeMember(playerUUID, data.getPlayersPowerRecord(playerUUID).getPowerLevel());
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(translate("&b" + getText("AlertForcedKick")));
        }
        if (faction.isOfficer(playerUUID)) faction.removeOfficer(playerUUID); // Remove Officer (if they are one).
        sender.sendMessage(translate("&a" + getText("SuccessFactionMemberRemoval")));
    }

    private void forcePower(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.power", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 4)) {
            sender.sendMessage(translate("&c" + getText("UsageForcePower")));
            return;
        }
        // get arguments designated by single quotes
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + getText("PlayerAndDesiredPowerSingleQuotesRequirement")));
            return;
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(singleQuoteArgs.get(0));
        final int desiredPower = getIntSafe(singleQuoteArgs.get(1), Integer.MIN_VALUE);
        if (desiredPower == Integer.MIN_VALUE) {
            sender.sendMessage(translate("&c" + getText("DesiredPowerMustBeANumber")));
            return;
        }
        final PlayerPowerRecord record = data.getPlayersPowerRecord(playerUUID);
        record.setPowerLevel(desiredPower); // Set power :)
        sender.sendMessage(translate("&a" + getText("PowerLevelHasBeenSetTo", desiredPower)));
    }

    private void forceRenounce(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.renounce", "mf.force.*", "mf.admin"))) return;
        if (args.length < 2) {
            sender.sendMessage(translate("&c" + getText("UsageForceRenounce")));
            return;
        }
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        // single quote args length check
        if (singleQuoteArgs.size() != 1) {
            sender.sendMessage(translate("&c" + getText("FactionMustBeDesignatedInsideSingleQuotes")));
            return;
        }
        final String factionName = singleQuoteArgs.get(0);
        final Faction faction = getFaction(factionName);
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        long changes = data.getFactions().stream()
                .filter(f -> f.isLiege(factionName) || f.isVassal(factionName))
                .count(); // Count changes

        data.getFactions().stream().filter(f -> f.isLiege(factionName)).forEach(f -> f.setLiege("none"));
        data.getFactions().stream().filter(f -> f.isVassal(factionName)).forEach(Faction::clearVassals);
        if (!faction.getLiege().equalsIgnoreCase("none")) {
            faction.setLiege("none");
            changes++;
        }
        if (faction.getNumVassals() != 0) {
            changes = changes + faction.getNumVassals();
            faction.clearVassals();
        }
        if (changes == 0) sender.sendMessage(translate("&a" + getText("NoVassalOrLiegeReferences")));
        else sender.sendMessage(translate("&a" + getText("SuccessReferencesRemoved")));
    }

    private void forceTransfer(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.transfer", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 4)) {
            sender.sendMessage(translate("&c" + getText("UsageForceTransfer")));
            return;
        }
        // get arguments designated by single quotes
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + getText("FactionAndPlayerSingleQuotesRequirement")));
            return;
        }
        final Faction faction = PersistentData.getInstance().getFaction(singleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(singleQuoteArgs.get(1));
        if (playerUUID == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer player = Bukkit.getOfflinePlayer(playerUUID);
        if (!player.hasPlayedBefore()) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        if (faction.isOwner(playerUUID)) {
            sender.sendMessage(translate("&c" + getText("AlertPlayerAlreadyOwner")));
            return;
        }
        if (!faction.isMember(playerUUID)) {
            sender.sendMessage(translate("&c" + getText("AlertPlayerNotInFaction")));
            return;
        }
        if (faction.isOfficer(playerUUID)) faction.removeOfficer(playerUUID); // Remove Officer.
        faction.setOwner(playerUUID);

        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(translate("&a" + getText("OwnershipTransferred", faction.getName())));
        }
        sender.sendMessage(translate("&a" + getText("OwnerShipTransferredTo", player.getName())));
    }

    private void forceRemoveVassal(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.removevassal", "mf.force.*", "mf.admin"))) return;
        if (args.length < 4) {
            sender.sendMessage(translate("&c" + getText("UsageForceRemoveVassal")));
            return;
        }
        // get arguments designated by single quotes
        final ArrayList<String> singleQuoteArgs = parser.getArgumentsInsideSingleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + getText("FactionAndVassalSingleQuotesRequirement")));
            return;
        }
        final Faction liege = getFaction(singleQuoteArgs.get(0));
        final Faction vassal = getFaction(singleQuoteArgs.get(1));
        if (liege != null && vassal != null) {
            // remove vassal from liege
            if (liege.isVassal(vassal.getName())) liege.removeVassal(vassal.getName());
            // set liege to "none" for vassal (if faction exists)
            if (vassal.isLiege(liege.getName())) vassal.setLiege("none");
        }
        sender.sendMessage(translate("&a" + getText("Done")));
    }

    @Deprecated
    public boolean force(CommandSender sender, String[] args) {
        if (args.length > 1) {
            if (args[1].equalsIgnoreCase("save") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceSave"))) {
                /*return*/
                forceSave(sender, args);
            }

            if (args[1].equalsIgnoreCase("load") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceLoad"))) {
                /*return*/
                forceLoad(sender, args);
            }

            if (args[1].equalsIgnoreCase("peace") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForcePeace"))) {
                /*return*/
                forcePeace(sender, args);
            }

            if (args[1].equalsIgnoreCase("demote") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceDemote"))) {
                /*return*/
                forceDemote(sender, args);
            }

            if (args[1].equalsIgnoreCase("join") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceJoin"))) {
                /*return*/
                forceJoin(sender, args);
            }

            if (args[1].equalsIgnoreCase("kick") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceKick"))) {
                /*return*/
                forceKick(sender, args);
            }
            if (args[1].equalsIgnoreCase("power") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForcePower"))) {
                /*return*/
                forcePower(sender, args);
            }
            if (args[1].equalsIgnoreCase("renounce") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceRenounce"))) {
                /*return*/
                forceRenounce(sender, args);
            }
            if (args[1].equalsIgnoreCase("transfer") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceTransfer"))) {
                /*return*/
                forceTransfer(sender, args);
            }
            if (args[1].equalsIgnoreCase("removevassal") || args[1].equalsIgnoreCase(LocaleManager.getInstance().getText("CmdForceRemoveVassal"))) {
                /*return*/
                forceRemoveVassal(sender, args);
            }
            return true;
        }
        // show usages
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceSave"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceLoad"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForcePeace"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceDemote"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceJoin"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceKick"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForcePower"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceRenounce"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceTransfer"));
        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpForceRemoveVassal"));
        return false;
    }

}
