package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.UUID;

public class WhoCommand extends SubCommand {

    public WhoCommand() {
        super(new String[] {
                "Who", LOCALE_PREFIX + "CmdWho"
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
        final String permission = "mf.who";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageWho")));
            return;
        }
        final UUID targetUUID = UUIDChecker.getInstance().findUUIDBasedOnPlayerName(args[0]);
        if (targetUUID == null) {
            player.sendMessage(translate("&c" + getText("PlayerNotFound")));
            return;
        }
        final Faction temp = getPlayerFaction(targetUUID);
        if (temp == null) {
            player.sendMessage(translate("&c" + getText("PlayerIsNotInAFaction")));
            return;
        }
        Messenger.getInstance().sendFactionInfo(
                player,
                temp,
                chunks.getChunksClaimedByFaction(temp.getName(), data.getClaimedChunks())
        );
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
    public void sendInformation(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.who")) {
                if (args.length > 1) {
                    String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);
                    Faction faction = PersistentData.getInstance().getPlayersFaction(UUIDChecker.getInstance().findUUIDBasedOnPlayerName(name));
                    if (faction != null) {
                        Messenger.getInstance().sendFactionInfo(player, faction, ChunkManager.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerIsNotInAFaction"));
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageWho"));
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.who"));
            }
        }
    }

}
