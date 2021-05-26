package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

public class HelpCommand extends SubCommand {

    // Last page of help menus.
    private static final int LAST_PAGE = 6;
    private final HashMap<Integer, List<String>> helpPages = new HashMap<>();

    public HelpCommand() {
        super(new String[] {
                "help", LOCALE_PREFIX + "CmdHelp"
        }, false);
        helpPages.put(1, Arrays.asList("Help", "List", "Info", "Members", "Join", "Leave", "Create", "Invite", "Desc"));
        helpPages.put(2, Arrays.asList("Kick", "Transfer", "Disband", "DeclareWar", "MakePeace", "Invoke", "Claim", "Unclaim", "Unclaimall", "CheckClaim"));
        helpPages.put(3, Arrays.asList("Autoclaim", "Promote", "Demote", "Power", "SetHome", "Home", "Who", "Ally", "BreakAlliance"));
        helpPages.put(4, Arrays.asList("Rename", "Lock", "Unlock", "GrantAccess", "CheckAccess", "RevokeAccess", "Laws", "AddLaw", "RemoveLaw"));
        helpPages.put(5, Arrays.asList("EditLaw", "Chat", "Vassalize", "SwearFealty", "DeclareIndependence", "GrantIndependence", "GateCreate", "GateName", "GateList", "GateRemove", "GateCancel"));
        helpPages.put(6, Arrays.asList("DuelChallenge", "DuelAccept", "DuelCancel", "Prefix", "Bypass", "ConfigShow", "ConfigSet", "Force", "Version", "ResetPowerLevels"));
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
        if (!(checkPermissions(sender, "mf.help"))) return;
        int page = (args.length <= 0 ? 1 : getIntSafe(args[0], 1));
        if (page > LAST_PAGE) page = LAST_PAGE; // Upper Limit over LAST_PAGE
        if (page <= 0) page = 1; // Lower Limit to 0
        sender.sendMessage(translate("&b&l" + getText("CommandsPage" + page, LAST_PAGE)));
        helpPages.get(page).forEach(line -> sender.sendMessage(translate("&b" + getText("Help" + line))));
    }

}
