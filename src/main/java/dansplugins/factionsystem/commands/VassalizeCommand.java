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
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class VassalizeCommand extends SubCommand {
    private final Logger logger;

    public VassalizeCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, Logger logger, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "vassalize", LOCALE_PREFIX + "CmdVassalize"
        }, true, true, false, true, ["mf.vassalize"], localeService, persistentData, ephemeralData, chunkDataAccessor, dynmapIntegrator, configService, playerService, messageService);
        this.logger = logger;
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
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("UsageVassalize"),
                "UsageVassalize",
                false
            );
            return;
        }
        final Faction target = this.getFaction(String.join(" ", args));
        if (target == null) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("FactionNotFound"),
                Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                true
            );
            return;
        }
        // make sure player isn't trying to vassalize their own faction
        if (this.faction.getName().equalsIgnoreCase(target.getName())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotVassalizeSelf"),
                "CannotVassalizeSelf",
                false
            );
            return;
        }
        // make sure player isn't trying to vassalize their liege
        if (target.getName().equalsIgnoreCase(faction.getLiege())) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotVassalizeLiege"),
                "CannotVassalizeLiege",
                false
            );
            return;
        }
        // make sure player isn't trying to vassalize a vassal
        if (target.hasLiege()) {
            this.playerService.sendMessage(
                player,
                "&c" + this.getText("CannotVassalizeVassal"),
                "CannotVassalizeVassal",
                false
            );
            return;
        }
        // make sure this vassalization won't result in a vassalization loop
        final int loopCheck = this.willVassalizationResultInLoop(faction, target);
        if (loopCheck == 1 || loopCheck == 2) {
            this.logger.debug("Vassalization was cancelled due to potential loop");
            return;
        }
        // add faction to attemptedVassalizations
        this.faction.addAttemptedVassalization(target.getName());

        // inform all players in that faction that they are trying to be vassalized
        this.messageFaction(
            target, 
            this.translate("&a" + this.getText("AlertAttemptedVassalization", this.faction.getName(), this.faction.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertAttemptedVassalization"))
                .replace("#name#", this.faction.getName())
        );

        // inform all players in players faction that a vassalization offer was sent
        this.messageFaction(
            this.faction,
            this.translate("&a" + this.getText("AlertFactionAttemptedToVassalize", target.getName())),
            Objects.requireNonNull(this.messageService.getLanguage().getString("AlertFactionAttemptedToVassalize"))
                .replace("#name#", target.getName())
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

    private int willVassalizationResultInLoop(Faction vassalizer, Faction potentialVassal) {
        final int MAX_STEPS = 1000;
        Faction current = vassalizer;
        int steps = 0;
        while (current != null && steps < MAX_STEPS) { // Prevents infinite loop and NPE (getFaction can return null).
            String liegeName = current.getLiege();
            if (liegeName.equalsIgnoreCase("none")) return 0; // no loop will be formed
            if (liegeName.equalsIgnoreCase(potentialVassal.getName())) return 1; // loop will be formed
            current = this.persistentData.getFaction(liegeName);
            steps++;
        }
        return 2; // We don't know :/
    }

    /**
     * Method to handle tab completion.
     * 
     * @param sender who sent the command.
     * @param args   of the command.
     */
    @Override
    public List<String> handleTabComplete(Sender sender, String[] args) {
        if (this.persistentData.isInFaction(sender.getUniqueId())) {
            Faction playerFaction = this.persistentData.getPlayersFaction(sender.getUniqueId());
            ArrayList<String> vassalizeableFactions = new ArrayList<>();
            for (Faction faction : this.persistentData.getFactions()) {
                if (!playerFaction.getVassals().contains(faction.getName())) {
                    vassalizeableFactions.add(faction.getName());
                }
            }
            return TabCompleteTools.filterStartingWith(args[0], vassalizeableFactions);
        }
    }
    
}