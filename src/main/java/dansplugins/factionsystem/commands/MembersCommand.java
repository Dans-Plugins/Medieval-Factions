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

    public MembersCommand(LocaleService localeService, PersistentData persistentData, EphemeralData ephemeralData, PersistentData.ChunkDataAccessor chunkDataAccessor, DynmapIntegrator dynmapIntegrator, ConfigService configService) {
        super(new String[]{
                "members", LOCALE_PREFIX + "CmdMembers"
        }, false, persistentData, localeService, ephemeralData, configService, chunkDataAccessor, dynmapIntegrator);
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
                new PlayerService().sendMessageType(sender, getText("OnlyPlayersCanUseCommand")
                        , "OnlyPlayersCanUseCommand", false);
                return;
            }
            faction = getPlayerFaction(sender);
            if (faction == null) {
                new PlayerService().sendMessageType(sender, getText("AlertMustBeInFactionToUseCommand")
                        , "AlertMustBeInFactionToUseCommand", false);
                return;
            }
        } else {
            faction = getFaction(String.join(" ", args));
            if (faction == null) {
                new PlayerService().sendMessageType(sender, "&c" + getText("FactionNameNotRecognized"),
                        Objects.requireNonNull(new MessageService().getLanguage().getString("FactionNotFound"))
                                .replaceAll("#faction#", String.join(" ", args
                                )), true);
                return;
            }
        }
        // send Faction Members
        if (!new MedievalFactions().USE_NEW_LANGUAGE_FILE) {
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
            new PlayerService().sendMessageType(sender, "", Objects.requireNonNull(new MessageService().getLanguage().getString("MembersFaction.Title"))
                            .replaceAll("#faction#", faction.getName())
                    , true);
            faction.getMemberList().stream()
                    .map(Bukkit::getOfflinePlayer)
                    .map(player -> {
                        String rank = new MessageService().getLanguage().getString("MembersFaction.Member.Rank");
                        String color = new MessageService().getLanguage().getString("MembersFaction.Member.Color");
                        if (faction.isOfficer(player.getUniqueId())) {
                            rank = new MessageService().getLanguage().getString("MembersFaction.Officer.Rank");
                            color = new MessageService().getLanguage().getString("MembersFaction.Officer.Color");
                        }
                        if (faction.isOwner(player.getUniqueId())) {
                            rank = new MessageService().getLanguage().getString("MembersFaction.Owner.Rank");
                            color = new MessageService().getLanguage().getString("MembersFaction.Owner.Color");
                        }
                        return new PlayerService().colorize(Objects.requireNonNull(new MessageService().getLanguage().getString("MembersFaction.Message"))
                                .replaceAll("#color#", Objects.requireNonNull(color))
                                .replaceAll("#rank#", Objects.requireNonNull(rank))
                                .replaceAll("#name#", Objects.requireNonNull(player.getName())));
                    }).forEach(sender::sendMessage);
            new PlayerService().sendMessageType(sender, "", Objects.requireNonNull(new MessageService().getLanguage().getString("MembersFaction.SubTitle"))
                            .replaceAll("#faction#", faction.getName())
                    , true);

        }
    }
}