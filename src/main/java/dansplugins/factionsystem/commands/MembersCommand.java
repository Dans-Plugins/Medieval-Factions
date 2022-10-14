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
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import dansplugins.factionsystem.utils.TabCompleteTools;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.Objects;

/**
 * @author Callum Johnson
 */
public class MembersCommand extends SubCommand {
    private final MedievalFactions medievalFactions;

    public MembersCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService, PlayerService playerService, MessageService messageService, MedievalFactions medievalFactions) {
        super(new String[]{
                "members", LOCALE_PREFIX + "CmdMembers"
        }, false, ["mf.members"], persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final Faction faction;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                this.playerService.sendMessage(
                    sender,
                    this.getText("OnlyPlayersCanUseCommand"),
                    "OnlyPlayersCanUseCommand",
                    false
                );
                return;
            }
            faction = getPlayerFaction(sender);
            if (faction == null) {
                this.playerService.sendMessage(
                    sender, 
                    this.getText("AlertMustBeInFactionToUseCommand"),
                    "AlertMustBeInFactionToUseCommand",
                    false
                );
                return;
            }
        } else {
            faction = this.getFaction(String.join(" ", args));
            if (faction == null) {
                this.playerService.sendMessage(
                    sender,
                    "&c" + this.getText("FactionNameNotRecognized"),
                    Objects.requireNonNull(this.messageService.getLanguage().getString("FactionNotFound")).replace("#faction#", String.join(" ", args)),
                    true
                );
                return;
            }
        }
        // send Faction Members
        if (!this.configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(this.translate("&b----------\n" + getText("MembersOf", faction.getName())));
            sender.sendMessage(this.translate("&b----------\n"));
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = "";
                        String color = "&a";
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = "*";
                            color = "&b";
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = "**";
                            color = "&c";
                        }
                        return this.translate("&f" + player.getName() + color + rank);
                    }).forEach(sender::sendMessage);
            sender.sendMessage(this.translate("&b----------\n"));
        } else {
            this.playerService.sendMessage(
                sender,
                "",
                Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.Title")).replace("#faction#", faction.getName()),
                true
            );
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = this.messageService.getLanguage().getString("MembersFaction.Member.Rank");
                        String color = this.messageService.getLanguage().getString("MembersFaction.Member.Color");
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = this.messageService.getLanguage().getString("MembersFaction.Officer.Rank");
                            color = this.messageService.getLanguage().getString("MembersFaction.Officer.Color");
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = this.messageService.getLanguage().getString("MembersFaction.Owner.Rank");
                            color = this.messageService.getLanguage().getString("MembersFaction.Owner.Color");
                        }
                        return this.playerService.colorize(Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.Message"))
                                .replace("#color#", Objects.requireNonNull(color))
                                .replace("#rank#", Objects.requireNonNull(rank))
                                .replace("#name#", Objects.requireNonNull(player.getName())));
                    }).forEach(sender::sendMessage);
            this.playerService.sendMessage(
                sender,
                "",
                Objects.requireNonNull(this.messageService.getLanguage().getString("MembersFaction.SubTitle")).replace("#faction#", faction.getName()),
                true
            );

        }
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