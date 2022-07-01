/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionWarStartEvent;
import dansplugins.factionsystem.integrators.DynmapIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

/**
 * @author Callum Johnson
 */
public class DeclareIndependenceCommand extends SubCommand {

    public DeclareIndependenceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "declareindependence", "di", LOCALE_PREFIX + "CmdDeclareIndependence"
        }, true, true, false, true, localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService);
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
        final String permission = "mf.declareindependence";
        if (!(checkPermissions(player, permission))) {
            return;
        }

        if (!(this.faction.hasLiege()) || this.faction.getLiege() == null) {
            player.sendMessage(translate("&c" + getText("NotAVassalOfAFaction")));
            return;
        }

        final Faction liege = getFaction(this.faction.getLiege());
        if (liege == null) {
            player.sendMessage(translate("&c" + getText("FactionNotFound")));
            return;
        }

        // break vassal agreement.
        liege.removeVassal(this.faction.getName());
        this.faction.setLiege("none");

        if (!configService.getBoolean("allowNeutrality") || (!((boolean) faction.getFlags().getFlag("neutral")) && !((boolean) liege.getFlags().getFlag("neutral")))) {
            // make enemies if (1) neutrality is disabled or (2) declaring faction is not neutral and liege is not neutral
            FactionWarStartEvent warStartEvent = new FactionWarStartEvent(this.faction, liege, player);
            Bukkit.getPluginManager().callEvent(warStartEvent);

            if (!warStartEvent.isCancelled()) {
                this.faction.addEnemy(liege.getName());
                liege.addEnemy(this.faction.getName());

                // break alliance if allied
                if (this.faction.isAlly(liege.getName())) {
                    this.faction.removeAlly(liege.getName());
                    liege.removeAlly(faction.getName());
                }
            }
        }
        messageServer(translate("&c" + getText("HasDeclaredIndependence", faction.getName(), liege.getName())));
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