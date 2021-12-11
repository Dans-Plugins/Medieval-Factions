package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionDisbandEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalChunkService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

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
        final int factionIndex = data.getFactions().indexOf(disband);
        if (self) {
            sender.sendMessage(translate("&a" + getText("FactionSuccessfullyDisbanded")));
            ephemeral.getPlayersInFactionChat().remove(((Player) sender).getUniqueId());
        }
        else sender.sendMessage(translate("&a" + getText("SuccessfulDisbandment", disband.getName())));
        removeFaction(factionIndex, self ? ((OfflinePlayer) sender) : null);
    }

    private void removeFaction(int i, OfflinePlayer disbandingPlayer) {

        Faction disbandingThisFaction = PersistentData.getInstance().getFactions().get(i);
        String nameOfFactionToRemove = disbandingThisFaction.getName();
        FactionDisbandEvent event = new FactionDisbandEvent(
                disbandingThisFaction,
                disbandingPlayer
        );
        Bukkit.getPluginManager().callEvent(event);
        if (event.isCancelled()) {
            // TODO: add locale message
            return;
        }

        // remove claimed land objects associated with this faction
        LocalChunkService.getInstance().removeAllClaimedChunks(nameOfFactionToRemove, PersistentData.getInstance().getClaimedChunks());
        DynmapIntegrator.getInstance().updateClaims();

        // remove locks associated with this faction
        PersistentData.getInstance().removeAllLocks(PersistentData.getInstance().getFactions().get(i).getName());


        for (Faction faction : PersistentData.getInstance().getFactions()) {

            // remove records of alliances/wars associated with this faction
            if (faction.isAlly(nameOfFactionToRemove)) {
                faction.removeAlly(nameOfFactionToRemove);
            }
            if (faction.isEnemy(nameOfFactionToRemove)) {
                faction.removeEnemy(nameOfFactionToRemove);
            }

            // remove liege and vassal references associated with this faction
            if (faction.isLiege(nameOfFactionToRemove)) {
                faction.setLiege("none");
            }

            if (faction.isVassal(nameOfFactionToRemove)) {
                faction.removeVassal(nameOfFactionToRemove);
            }

        }

        PersistentData.getInstance().getFactions().remove(i);
    }

}
