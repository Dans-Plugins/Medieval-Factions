package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class EditLawCommand extends SubCommand {

    public EditLawCommand() {
        super(new String[] {
            "EditLaw", "EL", LOCALE_PREFIX + "CmdEditLaw"
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
        final String permission = "mf.editlaw";
        if (!(checkPermissions(player, permission))) return;
        final int lawToEdit = getIntSafe(args[0], 0) - 1;
        if (lawToEdit < 0 || lawToEdit >= faction.getLaws().size()) {
            player.sendMessage(translate("&c" + getText("UsageEditLaw")));
            return;
        }
        String[] arguments = new String[args.length - 1];
        System.arraycopy(args, 1, arguments, 0, arguments.length);
        final String edittedLaw = String.join(" ", arguments);
        if (faction.editLaw(lawToEdit, edittedLaw)) {
            player.sendMessage(translate("&a" + getText("LawEdited")));
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
    public void editLaw(CommandSender sender, String[] args) {
        // player & perm check
        if (sender instanceof Player && ( ((Player) sender).hasPermission("mf.editlaw")) ) {

            Player player = (Player) sender;

            if (PersistentData.getInstance().isInFaction(player.getUniqueId())) {
                Faction playersFaction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

                if (playersFaction.isOwner(player.getUniqueId())) {
                    if (args.length > 1) {
                        int lawToEdit = Integer.parseInt(args[1]) - 1;
                        String newLaw = "";
                        for (int i = 2; i < args.length; i++) {
                            newLaw = newLaw + args[i] + " ";
                        }

                        if (playersFaction.editLaw(lawToEdit, newLaw)) {
                            player.sendMessage(ChatColor.GREEN + LocaleManager.getInstance().getText("LawEdited"));
                        }
                    }
                    else {
                        player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("UsageEditLaw"));
                    }

                }

            }
            else {
                player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertMustBeInFactionToUseCommand"));
            }
        }
        else {
            sender.sendMessage(ChatColor.RED + String.format(LocaleManager.getInstance().getText("PermissionNeeded"), "mf.editlaw"));
        }
    }

}
