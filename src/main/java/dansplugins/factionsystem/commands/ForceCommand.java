/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.events.FactionJoinEvent;
import dansplugins.factionsystem.events.FactionKickEvent;
import dansplugins.factionsystem.events.FactionRenameEvent;
import dansplugins.factionsystem.events.FactionWarEndEvent;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.fiefs.utils.UUIDChecker;
import preponderous.ponder.misc.ArgumentParser;

/**
 * @author Callum Johnson
 */
public class ForceCommand extends SubCommand {

    private final boolean debug = MedievalFactions.getInstance().isDebugEnabled();

    private final String[] commands = new String[]{
            "Save", "Load", "Peace", "Demote", "Join", "Kick", "Power", "Renounce", "Transfer", "RemoveVassal", "Rename", "BonusPower", "Unlock", "Create", "Claim", "Flag"
    };
    private final HashMap<List<String>, String> subMap = new HashMap<>();

    private final ArgumentParser argumentParser = new ArgumentParser();
    private final UUIDChecker uuidChecker = new UUIDChecker();

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
        if (!(args.length <= 0)) { // If the Argument has Arguments in the 'args' list.
            for (Map.Entry<List<String>, String> entry : subMap.entrySet()) { // Loop through the SubCommands.
                // Map.Entry<List<String>, String> example => ([Save, CMDForceSave (translation key)], forceSave)
                try {
                    if (safeEquals(args[0], entry.getKey().toArray(new String[0]))) { // Do any of the Keys for the SubCommand match the Given Argument at index '0'.
                        try {
                            Method method = getClass().getDeclaredMethod(entry.getValue(), CommandSender.class, String[].class); // Get the Declared method for that SubCommand.
                            method.invoke(this, sender, args);  // Use reflection to invoke the command.
                            // Due to the nature of the force-command, it is safe to use reflection here.
                        } catch (ReflectiveOperationException ex) {
                            System.out.println("DEBUG: Failed to resolve method from '" + args[0] + "'!");
                        }
                        return;
                    }
                } catch (Exception e) {
                    System.out.println("DEBUG: Failed to use safeEquals to determine the command chosen.");
                }
            }
        }
        // Print out these messages if the command either isn't found or if an error occurs or if the arguments ('args') list is empty.
        sender.sendMessage(translate("&b" + getText("SubCommands")));
        Arrays.stream(commands).forEach(str -> sender.sendMessage(translate("&b" + getText("HelpForce" + str))));
    }

    @SuppressWarnings("unused")
    private void forceSave(CommandSender sender) {
        if (!(checkPermissions(sender, "mf.force.save", "mf.force.*", "mf.admin"))) {
            return;
        }
        sender.sendMessage(translate("&a" + getText("AlertForcedSave")));
        PersistentData.getInstance().getLocalStorageService().save();
    }

    @SuppressWarnings("unused")
    private void forceLoad(CommandSender sender) {
        if (!(checkPermissions(sender, "mf.force.load", "mf.force.*", "mf.admin"))) {
            return;
        }
        sender.sendMessage(translate("&a" + LocalLocaleService.getInstance().getText("AlertForcedLoad")));
        PersistentData.getInstance().getLocalStorageService().load();
        MedievalFactions.getInstance().reloadConfig();
    }

    @SuppressWarnings("unused")
    private void forcePeace(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.peace", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (!(args.length >= 3)) {
            sender.sendMessage(translate("&c" + "Usage: /mf force peace \"faction1\" \"faction2\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction former = PersistentData.getInstance().getFaction(doubleQuoteArgs.get(0));
        final Faction latter = PersistentData.getInstance().getFaction(doubleQuoteArgs.get(1));
        if (former == null || latter == null) {
            sender.sendMessage(translate("&c" + getText("DesignatedFactionNotFound")));
            return;
        }
        FactionWarEndEvent warEndEvent = new FactionWarEndEvent(former, latter);
        Bukkit.getPluginManager().callEvent(warEndEvent);
        if (!warEndEvent.isCancelled()) {
            if (former.isEnemy(latter.getName())) former.removeEnemy(latter.getName());
            if (latter.isEnemy(former.getName())) latter.removeEnemy(former.getName());

            // announce peace to all players on server.
            messageServer(translate(
                    "&a" + getText("AlertNowAtPeaceWith", former.getName(), latter.getName())
            ));
        }
    }

    @SuppressWarnings("unused")
    private void forceDemote(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.demote", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 1)) {
            sender.sendMessage(translate("&c" + getText("UsageForceDemote")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(args[1]);
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

    @SuppressWarnings("unused")
    private void forceJoin(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.join", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(translate("&c" + "Usage: /mf force join \"player\" \"faction\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction faction = getFaction(doubleQuoteArgs.get(1));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(0));
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
        FactionJoinEvent joinEvent = new FactionJoinEvent(faction, player);
        Bukkit.getPluginManager().callEvent(joinEvent);
        if (joinEvent.isCancelled()) {
            Logger.getInstance().log("Join event was cancelled.");
            return;
        }
        messageFaction(faction, translate("&a" + getText("HasJoined", player.getName(), faction.getName())));
        faction.addMember(playerUUID);
        if (player.isOnline() && player.getPlayer() != null) {
            player.getPlayer().sendMessage(translate("&b" + getText("AlertForcedToJoinFaction")));
        }
        sender.sendMessage(translate("&a" + getText("SuccessForceJoin")));
    }

    @SuppressWarnings("unused")
    private void forceKick(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.kick", "mf.force.*", "mf.admin"))) return;
        if (!(args.length > 1)) {
            sender.sendMessage(translate("&c" + getText("UsageForceKick")));
            return;
        }
        if (debug) {
            System.out.printf("Looking for player UUID based on player name: '%s'%n", args[1]);
        }
        final UUID targetUUID = uuidChecker.findUUIDBasedOnPlayerName(args[1]);
        if (targetUUID == null) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final OfflinePlayer target = Bukkit.getOfflinePlayer(targetUUID);
        if (!target.hasPlayedBefore()) {
            sender.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final Faction faction = getPlayerFaction(target);
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        if (faction.isOwner(targetUUID)) {
            sender.sendMessage(translate("&c" + getText("CannotForciblyKickOwner")));
            return;
        }
        FactionKickEvent kickEvent = new FactionKickEvent(faction, target, null); // no kicker so null is used
        Bukkit.getPluginManager().callEvent(kickEvent);
        if (kickEvent.isCancelled()) {
            Logger.getInstance().log("Kick event was cancelled.");
            return;
        }
        if (faction.isOfficer(targetUUID)) {
            faction.removeOfficer(targetUUID); // Remove Officer (if one)
        }
        ephemeral.getPlayersInFactionChat().remove(targetUUID);
        faction.removeMember(targetUUID);
        messageFaction(faction, translate("&c" + getText("HasBeenKickedFrom", target.getName(), faction.getName())));
        if (target.isOnline() && target.getPlayer() != null) {
            target.getPlayer().sendMessage(translate("&c" + getText("AlertKicked", "an admin")));
        }
        sender.sendMessage(translate("&a" + getText("SuccessFactionMemberRemoval")));
    }

    @SuppressWarnings("unused")
    private void forcePower(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.power", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(translate("&c" + "Usage: /mf force power \"player\" \"number\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(0));
        final int desiredPower = getIntSafe(doubleQuoteArgs.get(1), Integer.MIN_VALUE);
        if (desiredPower == Integer.MIN_VALUE) {
            sender.sendMessage(translate("&c" + getText("DesiredPowerMustBeANumber")));
            return;
        }
        final PowerRecord record = data.getPlayersPowerRecord(playerUUID);
        record.setPower(desiredPower); // Set power :)
        sender.sendMessage(translate("&a" + getText("PowerLevelHasBeenSetTo", desiredPower)));
    }

    @SuppressWarnings("unused")
    private void forceRenounce(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.renounce", "mf.force.*", "mf.admin"))) return;
        if (args.length < 2) {
            sender.sendMessage(translate("&c" + "Usage: /mf force renounce \"faction\""));
            return;
        }
        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);

        if (doubleQuoteArgs.size() == 0) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final String factionName = doubleQuoteArgs.get(0);
        final Faction faction = getFaction(factionName);
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        long changes = PersistentData.getInstance().removeLiegeAndVassalReferencesToFaction(factionName);

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

    @SuppressWarnings("unused")
    private void forceTransfer(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.transfer", "mf.force.*", "mf.admin"))) return;
        if (!(args.length >= 3)) {
            sender.sendMessage(translate("&c" + "Usage: /mf force transfer \"faction\" \"player\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction faction = PersistentData.getInstance().getFaction(doubleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final UUID playerUUID = uuidChecker.findUUIDBasedOnPlayerName(doubleQuoteArgs.get(1));
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

    @SuppressWarnings("unused")
    private void forceRemoveVassal(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.removevassal", "mf.force.*", "mf.admin"))) return;
        if (args.length < 3) {
            sender.sendMessage(translate("&c" + "Usage: /mf force removevassal \"liege\" \"vassal\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        final Faction liege = getFaction(doubleQuoteArgs.get(0));
        final Faction vassal = getFaction(doubleQuoteArgs.get(1));
        if (liege != null && vassal != null) {
            // remove vassal from liege
            if (liege.isVassal(vassal.getName())) liege.removeVassal(vassal.getName());
            // set liege to "none" for vassal (if faction exists)
            if (vassal.isLiege(liege.getName())) vassal.setLiege("none");
        }
        sender.sendMessage(translate("&a" + getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceRename(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.rename", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(translate("&c" + "Usage: /mf force rename \"faction\" \"new name\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        Faction faction = getFaction(doubleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final String newName = doubleQuoteArgs.get(1);
        final String oldName = faction.getName();
        // rename faction
        if (getFaction(newName) != null) {
            sender.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }
        final FactionRenameEvent renameEvent = new FactionRenameEvent(faction, oldName, newName);
        Bukkit.getPluginManager().callEvent(renameEvent);
        if (renameEvent.isCancelled()) {
            Logger.getInstance().log("Rename event was cancelled.");
            return;
        }

        // change name
        faction.setName(newName);
        sender.sendMessage(translate("&a" + getText("FactionNameChanged")));

        PersistentData.getInstance().updateFactionReferencesDueToNameChange(oldName, newName);

        // Prefix (if it was unset)
        if (faction.getPrefix().equalsIgnoreCase(oldName)) faction.setPrefix(newName);

        // Save again to overwrite current data
        PersistentData.getInstance().getLocalStorageService().save();
    }

    @SuppressWarnings("unused")
    private void forceBonusPower(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.bonuspower", "mf.force.*", "mf.admin"))) {
            return;
        }
        if (args.length < 3) {
            sender.sendMessage(translate("&c" + "Usage: /mf force bonuspower \"faction\" \"number\""));
            return;
        }

        final ArrayList<String> singleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (singleQuoteArgs.size() < 2) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        // get faction
        Faction faction = getFaction(singleQuoteArgs.get(0));
        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        // get bonus power
        final int bonusPower = getIntSafe(singleQuoteArgs.get(1), Integer.MIN_VALUE);
        if (bonusPower == Integer.MIN_VALUE) {
            sender.sendMessage(translate("&c" + getText("DesiredPowerMustBeANumber")));
            return;
        }

        // set bonus power
        faction.setBonusPower(bonusPower);

        // inform sender
        sender.sendMessage(translate("&a" + getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceUnlock(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;

        if (!(checkPermissions(player, "mf.force.unlock", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (args.length > 1 && args[1].equalsIgnoreCase("cancel")) {
            ephemeral.getUnlockingPlayers().remove(player.getUniqueId());
            ephemeral.getForcefullyUnlockingPlayers().remove(player.getUniqueId());
            player.sendMessage(translate("&c" + getText("AlertUnlockingCancelled")));
            return;
        }
        if (!ephemeral.getUnlockingPlayers().contains(player.getUniqueId())) {
            // add player to playersAboutToLockSomething list
            ephemeral.getUnlockingPlayers().add(player.getUniqueId());
        }

        if (!ephemeral.getForcefullyUnlockingPlayers().contains(player.getUniqueId())) {
            // add player to playersAboutToLockSomething list
            ephemeral.getForcefullyUnlockingPlayers().add(player.getUniqueId());
        }

        ephemeral.getLockingPlayers().remove(player.getUniqueId()); // Remove from locking

        // inform them they need to right click the block that they want to lock or type /mf lock cancel to cancel it
        player.sendMessage(translate("&a" + getText("RightClickForceUnlock")));
    }

    @SuppressWarnings("unused")
    public void forceCreate(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player player = (Player) sender;

        if (!(checkPermissions(player, "mf.force.create", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(translate("&c" + "Usage: /mf force create \"faction name\""));
            return;
        }

        final ArrayList<String> doubleQuoteArgs = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (doubleQuoteArgs.size() < 1) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        String newFactionName = doubleQuoteArgs.get(0);

        if (getFaction(newFactionName) != null) {
            player.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }

        this.faction = new Faction(newFactionName);
        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            data.addFaction(this.faction);
            player.sendMessage(translate("&a" + getText("FactionCreated")));
        }
    }

    @SuppressWarnings("unused")
    public void forceClaim(CommandSender sender, String[] args) {
        if (!(sender instanceof Player)) {
            return;
        }

        Player p = (Player) sender;

        if (!(p.hasPermission("mf.force.claim") || p.hasPermission("mf.force.*") || p.hasPermission("mf.admin"))) {
            return;
        }

        if (args.length < 2) {
            sender.sendMessage(translate("&c" + "Usage: /mf force claim \"faction\""));
            return;
        }

        final ArrayList<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 1) {
            sender.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }

        String factionName = argumentsInsideDoubleQuotes.get(0);

        Faction faction = PersistentData.getInstance().getFaction(factionName);

        if (faction == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        PersistentData.getInstance().getChunkDataAccessor().forceClaimAtPlayerLocation(p, faction);
        sender.sendMessage(translate("&a" + getText("Done")));
    }

    @SuppressWarnings("unused")
    private void forceFlag(CommandSender sender, String[] args) {
        if (!(checkPermissions(sender, "mf.force.flag", "mf.force.*", "mf.admin"))) {
            return;
        }

        if (!(sender instanceof Player)) {
            sender.sendMessage("You must be a player ");
            return;
        }

        Player player = (Player) sender;

        if (args.length < 4) {
            player.sendMessage(translate("&c" + "Usage: /mf force flag \"faction\" \"flag\" \"value\""));
            return;
        }

        final ArrayList<String> argumentsInsideDoubleQuotes = argumentParser.getArgumentsInsideDoubleQuotes(args);
        if (argumentsInsideDoubleQuotes.size() < 3) {
            player.sendMessage(translate("&c" + "Arguments must be designated in between double quotes."));
            return;
        }
        Faction faction = getFaction(argumentsInsideDoubleQuotes.get(0));
        if (faction == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final String option = argumentsInsideDoubleQuotes.get(1);
        final String value = argumentsInsideDoubleQuotes.get(2);

        faction.getFlags().setFlag(option, value, player);
    }
}