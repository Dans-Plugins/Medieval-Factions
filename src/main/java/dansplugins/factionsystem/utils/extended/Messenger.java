/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.utils.extended;

import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.fiefs.externalapi.FI_Fief;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Daniel McCoy Stephenson
 */
public class Messenger extends preponderous.ponder.minecraft.bukkit.tools.Messenger {
    private static Messenger instance;

    private Messenger() {

    }

    public static Messenger getInstance() {
        if (instance == null) {
            instance = new Messenger();
        }
        return instance;
    }

    public void sendFactionInfo(CommandSender sender, Faction faction, int power) {
        UUIDChecker uuidChecker = new UUIDChecker();
        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("FactionInfo"), faction.getName()) + "\n----------\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Name"), faction.getName()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Owner"), uuidChecker.findPlayerNameBasedOnUUID(faction.getOwner())) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Description"), faction.getDescription()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Population"), faction.getMemberList().size()) + "\n");
        sendLiegeInfoIfVassal(faction, sender);
        sendFiefsInfo(faction, sender);
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("AlliedWith"), faction.getAlliesSeparatedByCommas()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("AtWarWith"), faction.getEnemiesSeparatedByCommas()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("PowerLevel"), faction.getCumulativePowerLevel()) + "/" + faction.getMaximumCumulativePowerLevel() + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("DemesneSize"), power, faction.getCumulativePowerLevel()) + "\n");
        sendLiegeInfoIfLiege(faction, sender);
        sendBonusPowerInfo(faction, sender);
        sender.sendMessage(ChatColor.AQUA + "----------\n");
    }

    private void sendBonusPowerInfo(Faction faction, CommandSender sender) {
        if (faction.getBonusPower() != 0) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("BonusPower"), faction.getBonusPower()));
        }
    }

    private void sendLiegeInfoIfLiege(Faction faction, CommandSender sender) {
        int vassalContribution = faction.calculateCumulativePowerLevelWithVassalContribution() - faction.calculateCumulativePowerLevelWithoutVassalContribution();
        if (faction.isLiege()) {
            if (!faction.isWeakened()) {
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("VassalContribution"), vassalContribution) + "\n");
            }
            else {
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("VassalContribution"), 0) + "\n");
            }
        }
    }

    private void sendFiefsInfo(Faction faction, CommandSender sender) {
        if (FiefsIntegrator.getInstance().isFiefsPresent()) {
            ArrayList<FI_Fief> fiefs = FiefsIntegrator.getInstance().getAPI().getFiefsOfFaction(faction.getName());
            if (fiefs.size() != 0) {
                StringBuilder fiefsSeparatedByCommas = new StringBuilder();
                for (FI_Fief fief : fiefs) {
                    fiefsSeparatedByCommas.append(fief.getName());
                }
                sender.sendMessage(ChatColor.AQUA + String.format("Fiefs: %s", fiefsSeparatedByCommas.toString()));
            }
        }
    }

    private void sendLiegeInfoIfVassal(Faction faction, CommandSender sender) {
        if (faction.hasLiege()) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Liege"), faction.getLiege()) + "\n");
        }
        if (faction.isLiege()) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Vassals"), faction.getVassalsSeparatedByCommas()) + "\n");
        }
    }

    public void sendAllPlayersInFactionMessage(Faction faction, String message) {
        ArrayList<UUID> members = faction.getMemberArrayList();
        for (UUID member : members) {
            try {
                Player target = getServer().getPlayer(member);
                if (target == null) {
                    continue;
                }
                target.sendMessage(message);
            }
            catch(Exception ignored) {

            }
        }
    }
}