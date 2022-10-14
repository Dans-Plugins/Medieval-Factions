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
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class BreakAllianceCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public BreakAllianceCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "breakalliance", "ba", LOCALE_PREFIX + "CmdBreakAlliance"
        }, true, true, false, true, ["mf.breakalliance"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
            this.playerService.sendMessage(player, "&c" + this.getText("UsageBreakAlliance"), "UsageBreakAlliance", false);
            return;
        }

        final Faction otherFaction = this.getFaction(String.join(" ", args));
        if (otherFaction == null) {
            this.playerService.sendMessage(player, "&c" + this.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound"))
                            .replace("#faction#", String.join(" ", args)), true);
            return;
        }

        if (otherFaction == this.faction) {
            this.playerService.sendMessage(player, "&c" + this.getText("CannotBreakAllianceWithSelf"), "CannotBreakAllianceWithSelf", false);
            return;
        }

        if (!this.faction.isAlly(otherFaction.getName())) {
            this.playerService.sendMessage(player, "&c" + this.getText("AlertNotAllied", otherFaction.getName()),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNotAllied"))
                            .replace("#faction#", otherFaction.getName()), true);
            return;
        }

        this.faction.removeAlly(otherFaction.getName());
        otherFaction.removeAlly(this.faction.getName());
        this.messageFaction(
            this.faction, 
            this.translate("&c" + this.getText("AllianceBrokenWith", otherFaction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AllianceBrokenWith"))
                .replace("#faction#", otherFaction.getName())
        );
        this.messageFaction(
            otherFaction, 
            this.translate("&c" + this.getText("AlertAllianceHasBeenBroken", this.faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAllianceHasBeenBroken"))
                .replace("#faction#", this.faction.getName())
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

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        final List<String> factionsAllowedtoAlly = new ArrayList<>();
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            ArrayList<String> playerAllies = playerFaction.getAllies();
            return TabCompleteTools.filterStartingWith(args[0], playerAllies);
        }
        return null;
    }
}