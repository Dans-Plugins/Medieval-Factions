package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.ChunkManager;
import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.Messenger;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.utils.ArgumentParser;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class InfoCommand extends SubCommand {

    public InfoCommand() {
        super(new String[] {
                "info", LOCALE_PREFIX + "CmdInfo"
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
        final String permission = "mf.info";
        if (!(checkPermissions(sender, permission))) return;
        final Faction target;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                sender.sendMessage(translate(getText("OnlyPlayersCanUseCommand")));
                return;
            }
            target = getPlayerFaction(sender);
        } else {
            target = getFaction(String.join(" ", args));
        }
        if (target == null) {
            sender.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }
        Messenger.getInstance().sendFactionInfo(sender, target, target.getClaimedChunks().size());
    }

    @Deprecated
    public void showInfo(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
           Player player = (Player) sender;

            if (sender.hasPermission("mf.info")) {
                if (args.length == 1) {
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.isMember(player.getUniqueId())) {
                            Messenger.getInstance().sendFactionInfo(player, faction, ChunkManager.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                            return;
                        }
                    }
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotInFaction"));
                }
                else {
                    // creating name from arguments 1 to the last one
                    String name = ArgumentParser.getInstance().createStringFromFirstArgOnwards(args);

                    boolean exists = false;
                    for (Faction faction : PersistentData.getInstance().getFactions()) {
                        if (faction.getName().equalsIgnoreCase(name)) {
                            exists = true;
                            Messenger.getInstance().sendFactionInfo(player, faction, ChunkManager.getInstance().getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()));
                        }
                    }
                    if (!exists) {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("FactionNotFound"));
                    }
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.info"));
            }
        }

    }

}
