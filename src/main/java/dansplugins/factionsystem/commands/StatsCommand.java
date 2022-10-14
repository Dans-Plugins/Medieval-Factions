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
    private PersistentData persistentData;

    public StatsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService, MedievalFactions medievalFactions) {
        super(new String[]{
                "stats", LOCALE_PREFIX + "CmdStats"
        }, false, false, false, false, [], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.medievalFactions = medievalFactions;
        this.persistentData = persistentData;
    }

    @Override
    public void execute(Player player, String[] args, String key) {
        execute((CommandSender) player, args, key);
    }

    @Override
    public void execute(CommandSender sender, String[] args, String key) {
        if (!this.configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(ChatColor.AQUA + "=== Medieval Factions Stats ===");
            sender.sendMessage(ChatColor.AQUA + "Number of factions: " + this.persistentData.getNumFactions());
            sender.sendMessage(ChatColor.AQUA + "Number of players: " + this.persistentData.getNumPlayers());
        } else {
            this.messageService.getLanguage().getStringList("StatsFaction")
                    .forEach(s -> {
                        if (s.contains("#faction#")) {
                            s = s.replace("#faction#", String.valueOf(this.persistentData.getNumFactions()));
                        }
                        if (s.contains("#players#")) {
                            s = s.replace("#players#", String.valueOf(this.persistentData.getNumPlayers()));
                        }
                        s = this.playerService.colorize(s);
                        this.playerService.sendMessage(sender, "", s, true);
                    });
        }
    }
}