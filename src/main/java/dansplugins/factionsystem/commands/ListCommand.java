package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.commands.abs.SubCommand;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class ListCommand extends SubCommand {

    public ListCommand() {
        super(new String[] {
                "list", LOCALE_PREFIX + "CmdList"
        }, false);
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
        final String permission = "mf.list";
        if (!(checkPermissions(sender, permission))) return;
        if (data.getFactions().size() == 0) {
            sender.sendMessage(translate("&b" + getText("CurrentlyNoFactions")));
            return;
        }
        sender.sendMessage(translate("&b&l" + getText("FactionsTitle")));
        List<SortableFaction> sortedFactionList = data.getFactions().stream()
                .map(fac -> new SortableFaction(fac, fac.getCumulativePowerLevel()))
                .sorted() // Sort the Factions by Power.
                .collect(Collectors.toList());
        sender.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("ListLegend"));
        sender.sendMessage(ChatColor.AQUA + "-----");
        for (SortableFaction sortableFaction : sortedFactionList) {
            final Faction temp = sortableFaction.getFaction();
            sender.sendMessage(ChatColor.AQUA + String.format("%-25s %10s %10s %10s", temp.getName(), "P: " +
                    temp.getCumulativePowerLevel(), "M: " + temp.getPopulation(), "L: " +
                    chunks.getChunksClaimedByFaction(temp.getName(), data.getClaimedChunks())));
        }
    }

    private static class SortableFaction implements Comparable<SortableFaction> {

        private final Faction faction;
        private final int power;

        public SortableFaction(Faction faction, int cumulativePower) {
            this.faction = faction;
            this.power = cumulativePower;
        }

        public Faction getFaction() {
            return faction;
        }

        public int getPower() {
            return power;
        }

        @Override
        public int compareTo(SortableFaction o) {
            int comparison = Integer.compare(getPower(), o.getPower()); // Current > Greater (higher first)

            // return the opposite of the result of the comparison so that factions will be sorted from highest to lowest power
            if (comparison > 0) {
                return -1;
            }
            else if (comparison < 0) {
                return 1;
            }
            else {
                return 0;
            }
        }

    }

}
