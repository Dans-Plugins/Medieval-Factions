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
        }, false, persistentData, localeService, ephemeralData, configService, playerService, messageService, chunkDataAccessor, dynmapIntegrator);
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
        final String permission = "mf.members";
        if (!(checkPermissions(sender, permission))) return;
        final Faction faction;
        if (args.length == 0) {
            if (!(sender instanceof Player)) {
                playerService.sendMessageType(sender, getText("OnlyPlayersCanUseCommand")
                        , "OnlyPlayersCanUseCommand", false);
                return;
            }
            faction = getPlayerFaction(sender);
            if (faction == null) {
                playerService.sendMessageType(sender, getText("AlertMustBeInFactionToUseCommand")
                        , "AlertMustBeInFactionToUseCommand", false);
                return;
            }
        } else {
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                playerService.sendMessageType(sender, "&c" + getText("FactionNameNotRecognized"),
                        Objects.requireNonNull(messageService.getLanguage().getString("FactionNotFound"))
                                .replaceAll("#faction#", String.join(" ", args
                                )), true);
                return;
            }
        }
        // send Faction Members
        if (!medievalFactions.USE_NEW_LANGUAGE_FILE) {
            sender.sendMessage(translate("&b----------\n" + getText("MembersOf", faction.getName())));
            sender.sendMessage(translate("&b----------\n"));
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
                        return translate("&f" + player.getName() + color + rank);
                    }).forEach(sender::sendMessage);
            sender.sendMessage(translate("&b----------\n"));
        } else {
            playerService.sendMessageType(sender, "", Objects.requireNonNull(messageService.getLanguage().getString("MembersFaction.Title"))
                            .replaceAll("#faction#", faction.getName())
                    , true);
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = messageService.getLanguage().getString("MembersFaction.Member.Rank");
                        String color = messageService.getLanguage().getString("MembersFaction.Member.Color");
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = messageService.getLanguage().getString("MembersFaction.Officer.Rank");
                            color = messageService.getLanguage().getString("MembersFaction.Officer.Color");
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = messageService.getLanguage().getString("MembersFaction.Owner.Rank");
                            color = messageService.getLanguage().getString("MembersFaction.Owner.Color");
                        }
                        return playerService.colorize(Objects.requireNonNull(messageService.getLanguage().getString("MembersFaction.Message"))
                                .replaceAll("#color#", Objects.requireNonNull(color))
                                .replaceAll("#rank#", Objects.requireNonNull(rank))
                                .replaceAll("#name#", Objects.requireNonNull(player.getName())));
                    }).forEach(sender::sendMessage);
            playerService.sendMessageType(sender, "", Objects.requireNonNull(messageService.getLanguage().getString("MembersFaction.SubTitle"))
                            .replaceAll("#faction#", faction.getName())
                    , true);

        }
    }
}