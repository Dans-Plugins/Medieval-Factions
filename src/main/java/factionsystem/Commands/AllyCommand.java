package factionsystem.Commands;

import factionsystem.Main;
import factionsystem.Objects.Faction;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import static factionsystem.Subsystems.UtilitySubsystem.*;

public class AllyCommand {

    Main main = null;

    public AllyCommand(Main plugin) {
        main = plugin;
    }

    public void requestAlliance(CommandSender sender, String[] args) {
        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (isInFaction(player.getUniqueId(), main.factions)) {
                Faction playersFaction = getPlayersFaction(player.getUniqueId(), main.factions);

                if (playersFaction.isOwner(player.getUniqueId()) || playersFaction.isOfficer(player.getUniqueId())) {

                    // player is able to do this command

                    if (args.length > 1) {
                        String targetFactionName = createStringFromFirstArgOnwards(args);
                        Faction targetFaction = getFaction(targetFactionName, main.factions);

                        if (!playersFaction.getName().equalsIgnoreCase(targetFactionName)) {

                            if (targetFaction != null) {

                                if (!playersFaction.isAlly(targetFactionName)) {
                                    // if not already ally

                                    if (!playersFaction.isRequestedAlly(targetFactionName)) {
                                        // if not already requested

                                        if (!playersFaction.isEnemy(targetFactionName)) {

                                            playersFaction.requestAlly(targetFactionName);
                                            player.sendMessage(ChatColor.GREEN + "▎Попытка вступить в союз с " + targetFactionName);

                                            sendAllPlayersInFactionMessage(targetFaction,ChatColor.GREEN + "" + playersFaction.getName() + " пытался заключить союз с " + targetFactionName + "!");

                                            if (playersFaction.isRequestedAlly(targetFactionName) && targetFaction.isRequestedAlly(playersFaction.getName())) {
                                                // ally factions
                                                playersFaction.addAlly(targetFactionName);
                                                getFaction(targetFactionName, main.factions).addAlly(playersFaction.getName());
                                                player.sendMessage(ChatColor.GREEN + "▎Ваша фракция теперь в союзе с" + targetFactionName + "!");
                                                sendAllPlayersInFactionMessage(targetFaction, ChatColor.GREEN + "▎Ваша фракция теперь в союзе с " + playersFaction.getName() + "!");
                                            }
                                        }
                                        else {
                                            player.sendMessage(ChatColor.RED + "▎Эта фракция сейчас ваш враг! Помиритесь, прежде чем пытаться объединиться с ними.");
                                        }

                                    }
                                    else {
                                        player.sendMessage(ChatColor.RED + "▎Вы уже запросили союз с этой фракцией!");
                                    }

                                }
                                else {
                                    player.sendMessage(ChatColor.RED + "▎Эта фракция уже является вашим союзником!");
                                }
                            }
                            else {
                                player.sendMessage(ChatColor.RED + "▎Эта фракция не найдена!");
                            }
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "▎Вы не можете вступить в союз с собственной фракцией?");
                        }

                    }
                    else {
                        player.sendMessage(ChatColor.RED + "▎Использование: /mf ally (имя фракции)");
                    }

                }
                else {
                    player.sendMessage(ChatColor.RED + "▎Вы должны быть владельцем фракции или офицером фракции, чтобы использовать эту команду.");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "▎Чтобы использовать эту команду, вы должны быть во фракции.");
            }
        }
    }
}
