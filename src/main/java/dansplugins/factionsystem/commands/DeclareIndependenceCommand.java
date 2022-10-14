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
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class DeclareIndependenceCommand extends SubCommand {

    public DeclareIndependenceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
            "declareindependence", "di", LOCALE_PREFIX + "CmdDeclareIndependence"
        }, true, true, false, true, ["mf.declareindependence"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
        if (!(this.faction.hasLiege()) || this.faction.getLiege() == null) {
            this.playerService.sendMessage(player, "&c" + this.getText("NotAVassalOfAFaction"), "NotAVassalOfAFaction", false);
            return;
        }

        final Faction liege = this.getFaction(this.faction.getLiege());
        if (liege == null) {
            this.playerService.sendMessage(
                player, 
                "&c" + getText("FactionNotFound"), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true
            );
            return;
        }

        // break vassal agreement.
        liege.removeVassal(this.faction.getName());
        this.faction.setLiege("none");

        if (!this.configService.getBoolean("allowNeutrality") || (!((boolean) this.faction.getFlags().getFlag("neutral")) && !((boolean) liege.getFlags().getFlag("neutral")))) {
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
        this.messageServer(
            "&c" + this.getText("HasDeclaredIndependence", this.faction.getName(), liege.getName()), 
            Objects.requireNonNull(this.messageService.getLanguage().getString("HasDeclaredIndependence"))
                .replace("#faction_a#", this.faction.getName())
                .replace("#faction_b#", liege.getName())
        );

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