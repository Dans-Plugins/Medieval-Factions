/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class FlagsCommand extends SubCommand {

    public FlagsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{"flags", LOCALE_PREFIX + "CmdFlags"}, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
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
        final String permission = "mf.flags";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (args.length == 0) {
            new PlayerService().sendMessageType(player, "&c" + getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);
            return;
        }

        final Faction playersFaction = getPlayerFaction(player);

        final boolean show = safeEquals(args[0], "get", "show", new PlayerService().getMessageType(getText("CmdFlagsShow"), new MessageService().getLanguage().getString("Alias.CmdFlagsShow")));
        final boolean set = safeEquals(args[0], "set", new PlayerService().getMessageType(getText("CmdFlagsSet"), new MessageService().getLanguage().getString("Alias.CmdFlagsSet")));
        if (show) {
            playersFaction.getFlags().sendFlagList(player);
        } else if (set) {
            if (args.length < 3) {
                new PlayerService().sendMessageType(player, "&c" + getText("UsageFlagsSet"), "UsageFlagsSet", false);
            } else {
                final StringBuilder builder = new StringBuilder(); // Send the flag_argument as one String
                for (int i = 2; i < args.length; i++) builder.append(args[i]).append(" ");
                playersFaction.getFlags().setFlag(args[1], builder.toString().trim(), player);

            }
        } else {
            new PlayerService().sendMessageType(player, "&c" + getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);

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