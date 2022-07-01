/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionCreateEvent;
import dansplugins.factionsystem.integrators.CurrenciesIntegrator;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class CreateCommand extends SubCommand {
    private final FiefsIntegrator fiefsIntegrator;
    private final CurrenciesIntegrator currenciesIntegrator;
    private final Logger logger;
    private final MedievalFactions medievalFactions;

    public CreateCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, FiefsIntegrator fiefsIntegrator, CurrenciesIntegrator currenciesIntegrator, Logger logger, MedievalFactions medievalFactions) {
        super(new String[]{
                LOCALE_PREFIX + "CmdCreate", "Create"
        }, true, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
        this.fiefsIntegrator = fiefsIntegrator;
        this.currenciesIntegrator = currenciesIntegrator;
        this.logger = logger;
        this.medievalFactions = medievalFactions;
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
        final String permission = "mf.create";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        this.faction = getPlayerFaction(player);
        if (this.faction != null) {
            player.sendMessage(translate("&c" + getText("AlreadyInFaction")));
            return;
        }

        if (args.length == 0) {
            player.sendMessage(translate("&c" + getText("UsageCreate")));
            return;
        }

        final String factionName = String.join(" ", args).trim();

        final FileConfiguration config = configService.getConfig();

        if (factionName.length() > config.getInt("factionMaxNameLength")) {
            player.sendMessage(translate("&c" + getText("FactionNameTooLong")));
            return;
        }

        if (persistentData.getFaction(factionName) != null) {
            player.sendMessage(translate("&c" + getText("FactionAlreadyExists")));
            return;
        }

        this.faction = new Faction(factionName, player.getUniqueId(), configService, localeService, fiefsIntegrator, currenciesIntegrator, dynmapIntegrator, logger, persistentData, medievalFactions);

        this.faction.addMember(player.getUniqueId());

        FactionCreateEvent createEvent = new FactionCreateEvent(this.faction, player);
        Bukkit.getPluginManager().callEvent(createEvent);
        if (!createEvent.isCancelled()) {
            persistentData.addFaction(this.faction);
            player.sendMessage(translate("&a" + getText("FactionCreated")));
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