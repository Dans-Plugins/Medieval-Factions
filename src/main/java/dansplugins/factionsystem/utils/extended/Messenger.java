/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.services.MessageService;
import dansplugins.factionsystem.services.PlayerService;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.ArrayList;
import java.util.Objects;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class Messenger extends preponderous.ponder.minecraft.bukkit.tools.Messenger {
    private final LocaleService localeService;
    private final PlayerService playerService;
    private final MessageService messageService;
    private final MedievalFactions medievalFactions;
    private final ConfigService configService;

    public Messenger(LocaleService localeService, PlayerService playerService, MessageService messageService, MedievalFactions medievalFactions, ConfigService configService) {
        this.localeService = localeService;
        this.playerService = playerService;
        this.messageService = messageService;
        this.medievalFactions = medievalFactions;
        this.configService = configService;
    }

    public void sendFactionInfo(CommandSender sender, Faction faction, int power) {
        UUIDChecker uuidChecker = new UUIDChecker();
        if (!configService.getBoolean("useNewLanguageFile")) {
            sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(localeService.get("FactionInfo"), faction.getName()) + "\n----------\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("Name"), faction.getName()) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("Owner"), uuidChecker.findPlayerNameBasedOnUUID(faction.getOwner())) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("Description"), faction.getDescription()) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("Population"), faction.getMemberList().size()) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("AlliedWith"), faction.getAlliesSeparatedByCommas()) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("AtWarWith"), faction.getEnemiesSeparatedByCommas()) + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("PowerLevel"), faction.getCumulativePowerLevel()) + "/" + faction.getMaximumCumulativePowerLevel() + "\n");
            sender.sendMessage(ChatColor.AQUA + String.format(localeService.get("DemesneSize"), power, faction.getCumulativePowerLevel()) + "\n");
            sender.sendMessage(ChatColor.AQUA + "----------\n");
        } else {
            messageService.getLanguage().getStringList("FactionInfo").forEach(s -> {
                s = s.replace("#faction#", faction.getName())
                        .replace("#name#", faction.getName())
                        .replace("#owner#", uuidChecker.findPlayerNameBasedOnUUID(faction.getOwner()))
                        .replace("#desc#", faction.getDescription())
                        .replace("#pplt#", String.valueOf(faction.getPopulation()))
                        .replace("#aw#", faction.getAlliesSeparatedByCommas())
                        .replace("#aww#", faction.getEnemiesSeparatedByCommas())
                        .replace("#pl#", String.valueOf(faction.getCumulativePowerLevel()))
                        .replace("#pl_max#", String.valueOf(faction.getMaximumCumulativePowerLevel()))
                        .replace("#number#", String.valueOf(power))
                        .replace("#max#", String.valueOf(faction.getCumulativePowerLevel()));
                playerService.sendMessage(sender, "", s, true);
            });
        }
        sendLiegeInfoIfVassal(faction, sender);
        sendLiegeInfoIfLiege(faction, sender);
        sendBonusPowerInfo(faction, sender);
    }

    private void sendBonusPowerInfo(Faction faction, CommandSender sender) {
        if (faction.getBonusPower() != 0) {
            playerService.sendMessage(sender, ChatColor.AQUA + String.format(localeService.get("BonusPower"), faction.getBonusPower())
                    , Objects.requireNonNull(messageService.getLanguage().getString("BonusPower")).replace("BonusPower", String.valueOf(faction.getBonusPower())), true);
        }
    }

    private void sendLiegeInfoIfLiege(Faction faction, CommandSender sender) {
        int vassalContribution = faction.calculateCumulativePowerLevelWithVassalContribution() - faction.calculateCumulativePowerLevelWithoutVassalContribution();
        if (faction.isLiege()) {
            if (!faction.isWeakened()) {
                playerService.sendMessage(sender, ChatColor.AQUA + String.format(localeService.get("VassalContribution"), vassalContribution) + "\n"
                        , Objects.requireNonNull(messageService.getLanguage().getString("VassalContribution")).replace("#amount#", String.valueOf(vassalContribution)), true);
            } else {
                playerService.sendMessage(sender, ChatColor.AQUA + String.format(localeService.get("VassalContribution"), 0) + "\n"
                        , Objects.requireNonNull(messageService.getLanguage().getString("VassalContribution")).replace("#amount#", String.valueOf(0)), true);
            }
        }
    }

    private void sendLiegeInfoIfVassal(Faction faction, CommandSender sender) {
        if (faction.hasLiege()) {
            playerService.sendMessage(sender, ChatColor.AQUA + String.format(localeService.get("Liege"), faction.getLiege()) + "\n"
                    , Objects.requireNonNull(messageService.getLanguage().getString("Liege")).replace("#name#", faction.getLiege()), true);
        }
        if (faction.isLiege()) {
            playerService.sendMessage(sender, ChatColor.AQUA + String.format(localeService.get("Vassals"), faction.getVassalsSeparatedByCommas()) + "\n"
                    , Objects.requireNonNull(messageService.getLanguage().getString("Vassals")).replace("#name#", faction.getVassalsSeparatedByCommas()), true);
        }
    }

    public void sendAllPlayersInFactionMessage(Faction faction, String message) {
        ArrayList<UUID> members = faction.getMemberArrayList();
        for (UUID member : members) {
            try {
                Player target = getServer().getPlayer(member);
                if (target != null) {
                    target.sendMessage(playerService.colorize(message));
                }
            } catch (Exception ignored) {

            }
        }
    }
}