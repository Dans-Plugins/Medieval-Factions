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

    public FlagsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
            "flags", LOCALE_PREFIX + "CmdFlags"
        }, true, true, false, true, ["mf.flags"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (args.length == 0) {
            this.playerService.sendMessage(player, "&c" + this.getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);
            return;
        }

        final Faction playersFaction = this.getPlayerFaction(player);

        final boolean show = this.safeEquals(args[0], "get", "show", 
            this.playerService.decideWhichMessageToUse(
                this.getText("CmdFlagsShow"), 
                this.messageService.getLanguage().getString("Alias.CmdFlagsShow")
            )
        );
        final boolean set = this.safeEquals(args[0], "set", 
            this.playerService.decideWhichMessageToUse(
                this.getText("CmdFlagsSet"), 
                this.messageService.getLanguage().getString("Alias.CmdFlagsSet")
            )
        );
        if (show) {
            playersFaction.getFlags().sendFlagList(player);
        } else if (set) {
            if (args.length < 3) {
                this.playerService.sendMessage(player, "&c" + this.getText("UsageFlagsSet"), "UsageFlagsSet", false);
            } else {
                final StringBuilder builder = new StringBuilder(); // Send the flag_argument as one String
                for (int i = 2; i < args.length; i++) builder.append(args[i]).append(" ");
                playersFaction.getFlags().setFlag(args[1], builder.toString().trim(), player);

            }
        } else {
            this.playerService.sendMessage(player, "&c" + this.getText("ValidSubCommandsShowSet"), "ValidSubCommandsShowSet", false);

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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (args.length == 1) {
            return TabCompleteTools.completeMultipleOptions(args[0], "set", "show");
        } else if (args.length == 2) {
            if (args[0] == "set") {
                if (this.persistentData.isInFaction(sender.getUniqueId())) {
                    Faction faction = this.persistentData.getPlayersFaction(player.getUniqueId());
                    return TabCompleteTools.filterStartingWith(args[1], faction.getFlags().getFlagNamesList());
                }
            }
        }
        return null;
    }
}