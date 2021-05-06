package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class CreateCommand extends SubCommand {

    public CreateCommand() {
        super(new String[] {
                LOCALE_PREFIX + "CmdCreate", "Create"
        }, true);
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
        final String permission = "mf.create";
        if (!(checkPermissions(player, permission))) return;
        this.faction = getPlayerFaction(player);
        if (this.faction != null) {
            player.sendMessage(translate("&c" + getText("AlreadyInFaction")));
            return;
        }
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageCreate")));
            return;
        }
        final String factionName = String.join(" ", args);
        this.faction = new Faction(factionName, player.getUniqueId(), getConfig().getInt("initialMaxPowerLevel"));
        this.faction.addMember(player.getUniqueId(), data.getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            data.getFactions().add(this.faction);
            player.sendMessage(translate("&c" + getText("FactionCreated")));
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

    @Deprecated
    public boolean createFaction(CommandSender sender, String[] args) {

        if (!(sender instanceof Player)) {
            sender.sendMessage(LocaleManager.getInstance().getText("OnlyPlayersCanUseCommand"));
            return false;
        }

        Player player = (Player) sender;

        if (!player.hasPermission("mf.create")) {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.create"));
            return false;
        }

        Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        if (playersFaction != null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlreadyInFaction"));
            return false;
        }

        if (args.length < 2) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageCreate"));
            return false;
        }

        String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

        if (PersistentData.getInstance().getFaction(name) != null) {
            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionAlreadyExists"));
            return false;
        }

        Faction temp = new Faction(name, player.getUniqueId(), MedievalFactions.getInstance().getConfig().getInt("initialMaxPowerLevel"));
        temp.addMember(player.getUniqueId(), PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        FactionCreateEvent createEvent = new FactionCreateEvent(temp, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            PersistentData.getInstance().getFactions().add(temp);
            player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("FactionCreated"));
        }
        return true;
    }

}
