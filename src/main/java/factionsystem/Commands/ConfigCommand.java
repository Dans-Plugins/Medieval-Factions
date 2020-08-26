package factionsystem.Commands;

import factionsystem.Main;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class ConfigCommand {

    Main main = null;

    public ConfigCommand(Main plugin) {
        main = plugin;
    }

    // args count is at least 1 at this point (/mf config)
    // possible sub-commands are show and set
    public void handleConfigAccess(CommandSender sender, String[] args) {

        if (sender instanceof Player) {
            Player player = (Player) sender;

            if (player.hasPermission("mf.config") || player.hasPermission("mf.admin")) {
                if (args.length > 1) {

                    if (args[1].equalsIgnoreCase("show")) {

                        // no further arguments needed, list config
                        player.sendMessage(ChatColor.AQUA + "version: " + main.getConfig().getString("version")
                                + ", maxPowerLevel: " + main.getConfig().getInt("maxPowerLevel")
                                + ", initialPowerLevel: " +  main.getConfig().getInt("initialPowerLevel")
                                + ", hourlyPowerIncreaseAmount: " + main.getConfig().getInt("hourlyPowerIncreaseAmount")
                                + ", mobsSpawnInFactionTerritory: " + main.getConfig().getBoolean("mobsSpawnInFactionTerritory")
                                + ", laddersPlaceableInEnemyFactionTerritory: " + main.getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")
                                + ", minutesBeforeInitialPowerIncrease: " + main.getConfig().getInt("minutesBeforeInitialPowerIncrease")
                                + ", minutesBetweenPowerIncreases: " + main.getConfig().getInt("minutesBetweenPowerIncreases")
                                + ", warsRequiredForPVP: " + main.getConfig().getBoolean("warsRequiredForPVP")
                                + ", officerLimit: " + main.getConfig().getInt("officerLimit")
                                + ", factionOwnerMultiplier: " + main.getConfig().getDouble("factionOwnerMultiplier"));

                        return;
                    }

                    if (args[1].equalsIgnoreCase("set")) {

                        // two more arguments needed
                        if (args.length > 3) {

                            String option = args[2];
                            String value = args[3];

                            setConfigOption(option, value, player);
                            return;
                        }
                        else {
                            player.sendMessage(ChatColor.RED + "Usage: /mf config set (option) (value)");
                            return;
                        }

                    }

                    player.sendMessage(ChatColor.RED + "Valid sub-commands: show, set");

                }
                else {
                    player.sendMessage(ChatColor.RED + "Valid sub-commands: show, set");
                }
            }
            else {
                player.sendMessage(ChatColor.RED + "Sorry! In order to use this command, you need the following permission: 'mf.config'");
            }
        }

    }

    private void setConfigOption(String option, String value, Player player) {

        if (main.getConfig().isSet(option)) {

            if (option.equalsIgnoreCase("version")) {
                player.sendMessage(ChatColor.RED + "Can't set version!");
                return;
            }
            else if (option.equalsIgnoreCase("maxPowerLevel") || option.equalsIgnoreCase("initialPowerLevel")
                    || option.equalsIgnoreCase("hourlyPowerIncreaseAmount")
                    || option.equalsIgnoreCase("minutesBeforeInitialPowerIncrease")
                    || option.equalsIgnoreCase("minutesBetweenPowerIncreases")
                    || option.equalsIgnoreCase("officerLimit")) {
                main.getConfig().set(option, Integer.parseInt(value));
                player.sendMessage(ChatColor.GREEN + "Integer set!");
                return;
            }
            else if (option.equalsIgnoreCase("mobsSpawnInFactionTerritory")
                    || option.equalsIgnoreCase("laddersPlaceableInEnemyFactionTerritory")
                    || option.equalsIgnoreCase("warsRequiredForPVP")) {
                main.getConfig().set(option, Boolean.parseBoolean(value));
                player.sendMessage(ChatColor.GREEN + "Boolean set!");
                return;
            }
            else if (option.equalsIgnoreCase("factionOwnerMultiplier")){
                main.getConfig().set(option, Double.parseDouble(value));
            }
            else {
                main.getConfig().set(option, value);
                player.sendMessage(ChatColor.GREEN + "String set!");
            }

            // save
            main.saveConfig();
        }

    }
}
