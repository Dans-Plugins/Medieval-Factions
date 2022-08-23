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

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class AddLawCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AddLawCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                LOCALE_PREFIX + "CMDAddLaw", "AL", "addlaw"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
    }

    /**
     * Method to execute the command.
     *
     * @param player who sent the command.
     * @param args   of the command.
     * @param key    of the command.
     */
    @Override
    public void execute(Player player, String[] args, String key) {
        final String permission = "mf.addlaw";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        // check if they have provided any strings beyond "addlaw"
        if (args.length == 0) {
            PlayerService.sendMessageType(player, translate("&c" + getText("UsageAddLaw")), "UsageAddLaw", false);
            return;
        }

        // add the law and send a success message.
        faction.addLaw(String.join(" ", args));
        PlayerService.sendMessageType(player, "&a" + getText("LawAdded"), Objects.requireNonNull(MessageService.getLanguage().getString("LawAdded"))
                .replaceAll("#law#", String.join(" ", args)), true);
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