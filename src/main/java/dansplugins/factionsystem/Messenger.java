package dansplugins.factionsystem;

import dansplugins.factionsystem.integrators.FiefsIntegrator;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.UUIDChecker;
import dansplugins.fiefs.externalapi.FI_Fief;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.UUID;

import static org.bukkit.Bukkit.getServer;

public class Messenger {

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
        int vassalContribution = faction.calculateCumulativePowerLevelWithVassalContribution() - faction.calculateCumulativePowerLevelWithoutVassalContribution();

        sender.sendMessage(ChatColor.BOLD + "" + ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("FactionInfo"), faction.getName()) + "\n----------\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Name"), faction.getName()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Owner"), UUIDChecker.getInstance().findPlayerNameBasedOnUUID(faction.getOwner())) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Description"), faction.getDescription()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Population"), faction.getMemberList().size()) + "\n");
        if (faction.hasLiege()) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Liege"), faction.getLiege()) + "\n");
        }
        if (faction.isLiege()) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("Vassals"), faction.getVassalsSeparatedByCommas()) + "\n");
        }
        if (FiefsIntegrator.getInstance().isFiefsPresent()) {
            ArrayList<FI_Fief> fiefs = FiefsIntegrator.getInstance().getAPI().getFiefsOfFaction(faction.getName());
            if (fiefs.size() != 0) {
                String fiefsSeparatedByCommas = "";
                for (FI_Fief fief : fiefs) {
                    fiefsSeparatedByCommas += fief.getName();
                }
                sender.sendMessage(ChatColor.AQUA + String.format("Fiefs: %s", fiefsSeparatedByCommas));
            }
        }
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("AlliedWith"), faction.getAlliesSeparatedByCommas()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("AtWarWith"), faction.getEnemiesSeparatedByCommas()) + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("PowerLevel"), faction.getCumulativePowerLevel()) + "/" + faction.getMaximumCumulativePowerLevel() + "\n");
        sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("DemesneSize"), power, faction.getCumulativePowerLevel()) + "\n");
        if (faction.isLiege()) {
            if (!faction.isWeakened()) {
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("VassalContribution"), vassalContribution) + "\n");
            }
            else {
                sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("VassalContribution"), 0) + "\n");
            }
        }
        if (faction.getBonusPower() != 0) {
            sender.sendMessage(ChatColor.AQUA + String.format(LocalLocaleService.getInstance().getText("BonusPower"), faction.getBonusPower()));
        }
        sender.sendMessage(ChatColor.AQUA + "----------\n");
    }

    public void sendAllPlayersInFactionMessage(Faction faction, String message) {
        ArrayList<UUID> members = faction.getMemberArrayList();
        for (UUID member : members) {
            try {
                Player target = getServer().getPlayer(member);
                target.sendMessage(message);
            }
            catch(Exception ignored) {

            }
        }
    }

    public void sendAllPlayersOnServerMessage(String message) {
        try {
            for (Player player : Bukkit.getOnlinePlayers()) {
                player.sendMessage(message);
            }
        }
        catch(Exception ignored) {

        }

    }

}
