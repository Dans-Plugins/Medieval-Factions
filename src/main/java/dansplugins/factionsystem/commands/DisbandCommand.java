/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionDisbandEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Daniel McCoy Stephenson
 * @author Callum Johnson
 */
public class DisbandCommand extends SubCommand {

    public DisbandCommand() {
        super(new String[]{
                "disband", LOCALE_PREFIX + "CmdDisband"
        }, false);
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
        final Faction disband;
        final boolean self;
        if (args.length == 0) {
            if (!checkPermissions(sender, "mf.disband")) return;
            if (!(sender instanceof Player)) { // ONLY Players can be in a Faction
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            disband = getPlayerFaction(sender);
            self = true;
            if (disband.getPopulation() != 1) {
                sender.sendMessage(translate("&c" + getText("AlertMustKickAllPlayers")));
                return;
            }
        } else {
            if (!checkPermissions(sender, "mf.disband.others", "mf.admin")) return;
            disband = getFaction(String.join(" ", args));
            self = false;
        }
        if (disband == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        final int factionIndex = data.getFactionIndexOf(disband);
        if (self) {
            sender.sendMessage(translate("&a" + getText("FactionSuccessfullyDisbanded")));
            ephemeral.getPlayersInFactionChat().remove(((Player) sender).getUniqueId());
        }
        else sender.sendMessage(translate("&a" + getText("SuccessfulDisbandment", disband.getName())));
        removeFaction(factionIndex, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(int i, OfflinePlayer disbandingPlayer) {

        Faction disbandingThisFaction = PersistentData.getInstance().getFactionByIndex(i);
        String nameOfFactionToRemove = disbandingThisFaction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                disbandingThisFaction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            Logger.getInstance().log("Disband event was cancelled.");
            return;
        }

        // remove claimed land objects associated with this faction
        PersistentData.getInstance().getChunkDataAccessor().removeAllClaimedChunks(nameOfFactionToRemove);
        DynmapIntegrator.getInstance().updateClaims();

        // remove locks associated with this faction
        PersistentData.getInstance().removeAllLocks(PersistentData.getInstance().getFactionByIndex(i).getName());

        PersistentData.getInstance().removePoliticalTiesToFaction(nameOfFactionToRemove);

        PersistentData.getInstance().removeFactionByIndex(i);
    }
}