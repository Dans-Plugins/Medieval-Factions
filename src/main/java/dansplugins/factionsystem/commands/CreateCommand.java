package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.objects.domain.Faction;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
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
        if (!(checkPermissions(player, permission))) {
            return;
        }

        this.faction = getPlayerFaction(player);
        if (this.faction != null) {
            player.sendMessage(translate("&c" + getText("AlreadyInFaction")));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageCreate")));
            return;
        }

        final String factionName = String.join(" ", args).trim();

        final FileConfiguration config = MedievalFactions.getInstance().getConfig();

        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            player.sendMessage(translate("&c" + getText("FactionNameTooLong")));
            return;
        }

        if (data.getFaction(factionName) != null) {
            player.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }

        this.faction = new Faction(factionName, player.getUniqueId());

        this.faction.addMember(player.getUniqueId());

        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            data.getFactions().add(this.faction);
            player.sendMessage(translate("&a" + getText("FactionCreated")));
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

}
