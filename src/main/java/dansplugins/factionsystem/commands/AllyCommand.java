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
public class AllyCommand extends SubCommand {

    /**
     * Constructor to initialise a Command.
     */
    public AllyCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "ally", LOCALE_PREFIX + "CmdAlly"
        }, true, true, true, false, ["mf.ally"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
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
            this.playerService.sendMessage(player, "&c" + getText("UsageAlly"), "UsageAlly", false);
            return;
        }

        // retrieve the Faction from the given arguments
        final Faction otherFaction = this.getFaction(String.join(" ", args));

        // the faction needs to exist to ally
        if (otherFaction == null) {
            this.playerService.sendMessage(player, "&c" + this.getText("FactionNotFound"), Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound"))
                    .replace("#faction#", String.join(" ", args)), true);
            return;
        }

        // the faction can't be itself
        if (otherFaction == this.faction) {
            this.playerService.sendMessage(player, "&c" + this.getText("CannotAllyWithSelf"), "CannotAllyWithSelf", false);
            return;
        }

        // no need to allow them to ally if they're already allies
        if (this.faction.isAlly(otherFaction.getName())) {
            this.playerService.sendMessage(player, "&c" + this.getText("FactionAlreadyAlly"), "FactionAlreadyAlly", false);
            return;
        }

        if (this.faction.isEnemy(otherFaction.getName())) {
            this.playerService.sendMessage(player, "&cThat faction is currently at war with your faction.", "FactionIsEnemy", false);
            return;
        }

        if (this.faction.isRequestedAlly(otherFaction.getName())) {
            this.playerService.sendMessage(player, "&c" + this.getText("AlertAlreadyRequestedAlliance"), "AlertAlreadyRequestedAlliance", false);
            return;
        }

        // send the request
        this.faction.requestAlly(otherFaction.getName());

        this.messageFaction(
                this.faction,
                this.translate("&a" + getText("AlertAttemptedAlliance", this.faction.getName(), otherFaction.getName())),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedAlliance"))
                        .replace("#faction_a#", this.faction.getName())
                        .replace("#faction_b#", otherFaction.getName())
        );

        this.messageFaction(
                otherFaction,
                this.translate("&a" + getText("AlertAttemptedAlliance", this.faction.getName(), otherFaction.getName())),
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedAlliance"))
                        .replace("#faction_a#", this.faction.getName())
                        .replace("#faction_b#", otherFaction.getName())
        );

        // check if both factions are have requested an alliance
        if (this.faction.isRequestedAlly(otherFaction.getName()) && otherFaction.isRequestedAlly(this.faction.getName())) {
            // ally them
            this.faction.addAlly(otherFaction.getName());
            otherFaction.addAlly(this.faction.getName());
            // message player's faction
            this.messageFaction(
                this.faction, 
                this.translate("&a" + getText("AlertNowAlliedWith", otherFaction.getName())), 
                Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAlliedWith")).replace("#faction#", otherFaction.getName())
            );

            // message target faction
            this.messageFaction(
                otherFaction, 
                this.translate("&a" + this.getText("AlertNowAlliedWith", this.faction.getName())), Objects.requireNonNull(this.messageService.getLanguage().getString("AlertNowAlliedWith")).replace("#faction#", this.faction.getName())
            );

            // remove alliance requests
            this.faction.removeAllianceRequest(otherFaction.getName());
            otherFaction.removeAllianceRequest(this.faction.getName());
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
        final List<String> factionsAllowedtoAlly = new ArrayList<>();
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            ArrayList<String> playerAllies = playerFaction.getAllies();
            for(Faction faction : this.persistentData.getFactions()) {
                if(!playerAllies.contains(faction.getName()) && !faction.getName().equals(playerFaction.getName())) {
                    factionsAllowedtoAlly.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], factionsAllowedtoAlly);
        }
        return null;
    }
}