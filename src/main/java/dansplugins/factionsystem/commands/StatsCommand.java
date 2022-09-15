/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class StatsCommand extends SubCommand {
    private MedievalFactions medievalFactions;

    public StatsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService, MedievalFactions medievalFactions) {
        super(new String[]{
                "stats", LOCALE_PREFIX + "CmdStats"
        }, false, false, false, false, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.medievalFactions = medievalFactions;
    }

    @Override
    public void execute(Player player, String[] args, String key) {
        execute((CommandSender) player, args, key);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        if (!configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(ChatColor.AQUA + "=== Medieval Factions Stats ===");
            sender.sendMessage(ChatColor.AQUA + "Number of factions: " + persistentData.getNumFactions());
        } else {
            messageService.getLanguage().getStringList("StatsFaction")
                    .forEach(s -> {
                        if (s.contains("#faction#")) {
                            s = s.replace("#faction#", String.valueOf(persistentData.getNumFactions()));
                        }
                        s = playerService.colorize(s);
                        playerService.sendMessageType(sender, "", s, true);
                    });
        }
    }
}