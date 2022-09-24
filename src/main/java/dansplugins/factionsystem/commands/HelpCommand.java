/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

/**
 * @author Callum Johnson
 */
public class HelpCommand extends SubCommand {
    private static final int LAST_PAGE = 7;
    private final HashMap<Integer, List<String>> helpPages = new HashMap<>();

    public HelpCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "help", LOCALE_PREFIX + "CmdHelp"
        }, false, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);

        // there should be 9 commands per page
        helpPages.put(1, Arrays.asList("Help", "List", "Info", "Members", "Join", "Leave", "Create", "Invite", "Desc"));
        helpPages.put(2, Arrays.asList("FlagsShow", "FlagsSet", "Kick", "Transfer", "Disband", "DeclareWar", "MakePeace", "Invoke", "Claim"));
        helpPages.put(3, Arrays.asList("Unclaim", "Unclaimall", "CheckClaim", "Autoclaim", "Promote", "Demote", "Power", "SetHome", "Home"));
        helpPages.put(4, Arrays.asList("Who", "Ally", "BreakAlliance", "Rename", "Lock", "Unlock", "GrantAccess", "CheckAccess", "RevokeAccess"));
        helpPages.put(5, Arrays.asList("Laws", "AddLaw", "RemoveLaw", "EditLaw", "Chat", "Vassalize", "SwearFealty", "DeclareIndependence", "GrantIndependence"));
        helpPages.put(6, Arrays.asList("GateCreate", "GateName", "GateList", "GateRemove", "GateCancel", "DuelChallenge", "DuelAccept", "DuelCancel", "Prefix"));
        helpPages.put(7, Arrays.asList("Map", "Bypass", "ConfigShow", "ConfigSet", "Force", "Version", "ResetPowerLevels"));
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
        if (!(checkPermissions(sender, "mf.help"))) {
            return;
        }
        int page = (args.length == 0 ? 1 : getIntSafe(args[0], 1));
        if (page > LAST_PAGE) {
            page = LAST_PAGE; // Upper Limit over LAST_PAGE
        }
        if (page <= 0) {
            page = 1; // Lower Limit to 0
        }
        sender.sendMessage(translate("&b&l" + getText("CommandsPage" + page, LAST_PAGE)));
        helpPages.get(page).forEach(line -> sender.sendMessage(translate("&b" + getText("Help" + line))));
    }
}