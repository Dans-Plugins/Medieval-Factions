package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DescCommand extends SubCommand {

    public DescCommand() {
        super(new String[] {
                "Desc", "Description", LOCALE_PREFIX + "CmdDesc"
        }, true, true, false, true);
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
        final String permission = "mf.desc";
        if (!(checkPermissions(player, permission))) return;
        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageDesc")));
            return;
        }
        faction.setDescription(String.join(" ", args));
        player.sendMessage(translate("&b" + getText("DescriptionSet")));
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
    public boolean setDescription(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (sender.hasPermission("mf.desc")) {
                boolean owner = false;
                for (Faction faction : PersistentData.getInstance().getFactions()) {
                    if (faction.isOwner(player.getUniqueId())) {
                        owner = true;
                        if (args.length > 1) {

                            // set arg[1] - args[args.length-1] to be the description with spaces put in between
                            String newDesc = "";
                            for (int i = 1; i < args.length; i++) {
                                newDesc = newDesc + args[i];
                                if (!(i == args.length - 1)) {
                                    newDesc = newDesc + " ";
                                }
                            }

                            faction.setDescription(newDesc);
                            player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("DescriptionSet"));
                            return true;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageDesc"));
                            return false;
                        }
                    }
                }
                if (!owner) {
                    player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeOwnerToUseCommand"));
                    return false;
                }
            }
            else {
                sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.desc"));
                return false;
            }
        }
        return false;
    }

}
