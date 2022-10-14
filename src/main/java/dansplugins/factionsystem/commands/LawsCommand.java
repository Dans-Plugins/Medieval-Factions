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
import java.util.stream.IntStream;

/**
 * @author Callum Johnson
 */
public class LawsCommand extends SubCommand {

    public LawsCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService) {
        super(new String[]{
                "laws", LOCALE_PREFIX + "CmdLaws"
        }, true, ["mf.laws"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final Faction target;
        if (args.length == 0) {
            target = this.getPlayerFaction(player);
            if (target == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand",
                    false
                );
                return;
            }
            if (target.getNumLaws() == 0) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.getText("AlertNoLaws")
                    "AlertNoLaws",
                    false
                );
                return;
            }
        } else {
            target = this.getFaction(String.join(" ", args));
            if (target == null) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.getText("FactionNotFound"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                    true
                );
                return;
            }
            if (target.getNumLaws() == 0) {
                this.playerService.sendMessage(
                    player,
                    "&c" + this.getText("FactionDoesNotHaveLaws"),
                    "FactionDoesNotHaveLaws",
                    false
                );
                return;
            }
        }
        this.playerService.sendMessage(
            player,
            "&b" + this.getText("LawsTitle", target.getName()),
            Objects.requireNonNull(this.messageService.getLanguage().getString("LawsTitle")).replace("#name#", target.getName()),
            true
        );
        IntStream.range(0, target.getNumLaws())
                .mapToObj(i -> translate("&b" + (i + 1) + ". " + target.getLaws().get(i)))
                .forEach(player::sendMessage);
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
        return TabCompleteTools.allFactionsMatching(args[0], this.persistentData);
    }
}