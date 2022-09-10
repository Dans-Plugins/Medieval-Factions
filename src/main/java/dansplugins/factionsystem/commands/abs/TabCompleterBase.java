package dansplugins.factionsystem.commands.abs;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.services.ConfigService;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;

public class TabCompleterBase implements TabCompleter {
	private PersistentData persistentData;
	private ConfigService configService;

	public TabCompleterBase(PersistentData persistentData, ConfigService configService) {
		this.persistentData = persistentData;
		this.configService = configService;
	}

	public  List<String> getOnlinePlayers(String partialName) {
		return filterStartingWith(partialName, Bukkit.getOnlinePlayers().stream().map(OfflinePlayer::getName));
	}

	public String joinArgsBeyond(int index, String delim, String[] args) {
		++index;
		String[] data = new String[args.length - index];
		System.arraycopy(args, index, data, 0, data.length);
		return String.join(delim, data);
	}

	public List<String> filterStartingWith(String prefix, Stream<String> stream) {
		return stream.filter((s) -> {
			return s != null && !s.isEmpty() && s.toLowerCase().startsWith(prefix.toLowerCase());
		}).collect(Collectors.toList());
	}

	public List<String> filterStartingWith(String prefix, Collection<String> strings) {
		return filterStartingWith(prefix, strings.stream());
	}

	public List<String> tackOnBeginningAndEndQuotes(List<String> targetedList) {
		final List<String> changed = new ArrayList<>();
		for(String string : targetedList) {
			String changedString = '"' + string + '"';
			changed.add(changedString);
		}

		return changed;
	}

	public String removeBeginningAndEndQuotes(String targetedString) {
		return targetedString.replace("\"", "");
	}

	List<String> argsLength1 = new ArrayList<String>();

	@Override
	public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
		final List<String> factionsAllowedtoAlly = new ArrayList<>();
		final List<String> factionsAllowedtoWar = new ArrayList<>();
		final List<String> officersInFaction = new ArrayList<>();
		final List<String> membersInFaction = new ArrayList<>();
		final List<String> factionNames = new ArrayList<>();
		List<String> result = new ArrayList<String>();

		if (sender instanceof Player) {
			Player player = (Player) sender;
			if (argsLength1.isEmpty()) {
				argsLength1.addAll(Arrays.asList(
						"addlaw",
						"ally",
						"autoclaim",
						"breakalliance",
						"bypass",
						"chat",
						"checkaccess",
						"checkclaim",
						"claim",
						"create",
						"declareindependence",
						"declarewar",
						"demote",
						"desc",
						"disband",
						"duel",
						"editlaw",
						"flags",
						"force",
						"gate",
						"grantaccess",
						"grantindependence",
						"help",
						"home",
						"info",
						"invite",
						"invoke",
						"join",
						"kick",
						"laws",
						"leave",
						"list",
						"lock",
						"makepeace",
						"map",
						"members",
						"power",
						"prefix",
						"promote",
						"removelaw",
						"rename",
						"revokeaccess",
						"sethome",
						"stats",
						"swearfealty",
						"transfer",
						"unclaimall",
						"unclaim",
						"unlock",
						"vassalize"
				));
			}
			if (args.length == 1) {
				if(player.isOp()) {
					argsLength1.addAll(Arrays.asList("config", "resetpowerlevels", "version", "bypass"));
					for(String a : argsLength1)	{
						if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
							result.add(a);
						}
					}
				} else {
					for(String a : argsLength1)	{
						if(a.toLowerCase().startsWith(args[0].toLowerCase())) {
							result.add(a);
						}
					}
				}
				return result;
			}
			if (args.length == 2) {
				if (args[0].equalsIgnoreCase("ally")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> playerAllies = playerFaction.getAllies();
						for(Faction faction : persistentData.getFactions()) {
							if(!playerAllies.contains(faction.getName()) && !faction.getName().equals(playerFaction.getName())) {
								factionsAllowedtoAlly.add(faction.getName());
							}
						}
						return filterStartingWith(args[1], factionsAllowedtoAlly);
					}
				}
				if (args[0].equalsIgnoreCase("breakalliance")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						return filterStartingWith(args[1], playerFaction.getAllies());
					}
				}
				if (args[0].equalsIgnoreCase("checkaccess")) {
					return filterStartingWith(args[1], Collections.singletonList("cancel"));
				}
				if (args[0].equalsIgnoreCase("config") && sender.hasPermission("mf.admin") || args[0].equalsIgnoreCase("config") && sender.hasPermission("mf.config")) {
					return filterStartingWith(args[1], Arrays.asList("get", "show", "set"));
				}
				if (args[0].equalsIgnoreCase("declarewar")) {
					if(persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> playerEnemies = playerFaction.getEnemyFactions();
						ArrayList<String> playerAllies = playerFaction.getAllies();
						for(Faction faction : persistentData.getFactions()) {
							// If the faction is not an ally and they are not already enemied to them
							if(!playerAllies.contains(faction.getName()) && !playerEnemies.contains(faction.getName()) && !faction.getName().equalsIgnoreCase(playerFaction.getName())) {
								factionsAllowedtoWar.add(faction.getName());
							}
						}
						return filterStartingWith(args[1], tackOnBeginningAndEndQuotes(factionsAllowedtoWar));
					}
				}
				if (args[0].equalsIgnoreCase("demote")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						for (UUID uuid : playerFaction.getOfficerList()) {
							Player officer = Bukkit.getPlayer(uuid);
							if (officer != null) {
								officersInFaction.add(officer.getName());
							}
						}
						return filterStartingWith(args[1], officersInFaction);
					}
				}
				if (args[0].equalsIgnoreCase("disband")) {
					if (sender.hasPermission("mf.admin") || sender.hasPermission("mf.disband.others")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[1], factionNames);
					}
				}
				if (args[0].equalsIgnoreCase("duel")) {
					return filterStartingWith(args[1], Arrays.asList("challenge", "accept", "cancel"));
				}
				if (args[0].equalsIgnoreCase("editlaw")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						if (playerFaction.getNumLaws() != 0) {
							ArrayList<String> numbers = new ArrayList<>();
							for (int i = 1; i < playerFaction.getNumLaws() + 1; i++) {
								numbers.add(Integer.toString(i));
							}
							return filterStartingWith(args[1], numbers);
						}
					}
				}
				if (args[0].equalsIgnoreCase("flags")) {
					return filterStartingWith(args[1], Arrays.asList("set", "show"));
				}
				if (args[0].equalsIgnoreCase("force")) {
					if(sender.hasPermission("mf.force.*")) {
						return filterStartingWith(args[1], Arrays.asList("save", "load", "peace", "demote", "join", "kick", "power", "renounce", "transfer", "removevassal", "rename", "bonuspower", "unlock", "create", "claim", "flag"));
					}
				}
				if (args[0].equalsIgnoreCase("gate")) {
					return filterStartingWith(args[1], Arrays.asList("create", "name", "list", "remove", "cancel"));
				}
				if (args[0].equalsIgnoreCase("grantaccess")) {
					return filterStartingWith(args[1], getOnlinePlayers(args[1]));
				}
				if (args[0].equalsIgnoreCase("grantindependence")) {
					if(persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						return filterStartingWith(args[1], faction.getVassals());
					}
				}
				if (args[0].equalsIgnoreCase("help")) {
					return filterStartingWith(args[1], IntStream.range(1, 7).mapToObj(String::valueOf));
				}
				if (args[0].equalsIgnoreCase("info")) {
					persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
					return filterStartingWith(args[1], factionNames);
				}
				if (args[0].equalsIgnoreCase("invite")) {
					return filterStartingWith(args[1], getOnlinePlayers(args[1]));
				}
				if (args[0].equalsIgnoreCase("invoke")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> allies = faction.getAllies();
						return filterStartingWith(args[1], tackOnBeginningAndEndQuotes(allies));
					}
				}
				if (args[0].equalsIgnoreCase("invite")) {
					return filterStartingWith(args[1], getOnlinePlayers(args[1]));
				}
				if (args[0].equalsIgnoreCase("join")) {
					persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
					return filterStartingWith(args[1], factionNames);
				}
				if (args[0].equalsIgnoreCase("kick")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						for (UUID uuid : faction.getMemberList()) {
							Player member = Bukkit.getPlayer(uuid);
							if (member != null) {
								membersInFaction.add(member.getName());
							}
						}
						return filterStartingWith(args[1], membersInFaction);
					}
				}
				if (args[0].equalsIgnoreCase("lock")) {
					return filterStartingWith(args[1], Collections.singletonList("cancel"));
				}
				if (args[0].equalsIgnoreCase("makepeace")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> enemies = faction.getEnemyFactions();
						return filterStartingWith(args[1], enemies);
					}
				}
				if (args[0].equalsIgnoreCase("members")) {
					persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
					return filterStartingWith(args[1], factionNames);
				}
				if (args[0].equalsIgnoreCase("power")) {
					return filterStartingWith(args[1], getOnlinePlayers(args[1]));
				}
				if (args[0].equalsIgnoreCase("promote")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						for (UUID uuid : faction.getMemberList()) {
							Player member = Bukkit.getPlayer(uuid);
							if (member != null) {
								if (!faction.getOfficerList().contains(uuid)) {
									membersInFaction.add(member.getName());
								}
							}
						}
						return filterStartingWith(args[1], membersInFaction);
					}
				}
				if (args[0].equalsIgnoreCase("removelaw")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						if (playerFaction.getNumLaws() != 0) {
							ArrayList<String> numbers = new ArrayList<>();
							for (int i = 1; i < playerFaction.getNumLaws() + 1; i++) {
								numbers.add(Integer.toString(i));
							}
							return filterStartingWith(args[1], numbers);
						}
					}
				}
				if (args[0].equalsIgnoreCase("revokeaccess")) {
					List<String> addCancelString = getOnlinePlayers(args[1]);
					addCancelString.add("cancel");
					return filterStartingWith(args[1], addCancelString);
				}
				if (args[0].equalsIgnoreCase("swearfealty")) {
					persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
					return filterStartingWith(args[1], factionNames);
				}
				if (args[0].equalsIgnoreCase("transfer")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						for (UUID uuid : faction.getMemberList()) {
							Player member = Bukkit.getPlayer(uuid);
							if (member != null) {
								membersInFaction.add(member.getName());
							}
						}
						return filterStartingWith(args[1], membersInFaction);
					}
				}
				if (args[0].equalsIgnoreCase("unlock")) {
					return filterStartingWith(args[1], Collections.singletonList("cancel"));
				}
				if (args[0].equalsIgnoreCase("vassalize")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction playerFaction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> vassalizeableFactions = new ArrayList<>();
						for (Faction faction : persistentData.getFactions()) {
							if (!playerFaction.getVassals().contains(faction.getName())) {
								vassalizeableFactions.add(faction.getName());
							}
						}
						return filterStartingWith(args[1], vassalizeableFactions);
					}
				}
				if (args[0].equalsIgnoreCase("who")) {
					return filterStartingWith(args[1], getOnlinePlayers(args[1]));
				}
				return null;
			}

			if (args.length == 3) {
				if (args[0].equalsIgnoreCase("config") && sender.hasPermission("mf.admin") || args[0].equalsIgnoreCase("config") && sender.hasPermission("mf.config")) {
					if (args[1].equalsIgnoreCase("show")) {
						return filterStartingWith(args[2], Arrays.asList("1", "2"));
					}
					if (args[1].equalsIgnoreCase("get")) {
						return filterStartingWith(args[2], Arrays.asList("1", "2"));
					}
					if (args[1].equalsIgnoreCase("set")) {
						return filterStartingWith(args[2], configService.getStringConfigOptions());
					}
				}
				if (args[0].equalsIgnoreCase("duel")) {
					if (args[1].equalsIgnoreCase("challenge")) {
						return filterStartingWith(args[2], getOnlinePlayers(args[2]));
					}
				}
				if (args[0].equalsIgnoreCase("flags")) {
					if (args[1].equalsIgnoreCase("set")) {
						if (persistentData.isInFaction(player.getUniqueId())) {
							Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
							return filterStartingWith(args[2], faction.getFlags().getFlagNamesList());
						}
					}
				}
				if (args[0].equalsIgnoreCase("force")) {
					if (args[1].equalsIgnoreCase("peace")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("demote")) {
						return filterStartingWith(args[2], getOnlinePlayers(args[2]));
					}
					if (args[1].equalsIgnoreCase("join")) {
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("kick")) {
						return filterStartingWith(args[2], getOnlinePlayers(args[2]));
					}
					if (args[1].equalsIgnoreCase("power")) {
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("renounce")) {
						if (persistentData.isInFaction(player.getUniqueId())) {
							persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
							return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
						}
					}
					if (args[1].equalsIgnoreCase("transfer")) {
						if (persistentData.isInFaction(player.getUniqueId())) {
							persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
							return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
						}
					}
					if (args[1].equalsIgnoreCase("removevassal")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("rename")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("bonuspower")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("unlock")) {
						return filterStartingWith(args[2], Collections.singletonList("cancel"));
					}
					if (args[1].equalsIgnoreCase("claim")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("flag")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[2], tackOnBeginningAndEndQuotes(factionNames));
					}
				}
				if (args[0].equalsIgnoreCase("invoke")) {
					if (persistentData.isInFaction(player.getUniqueId())) {
						Faction faction = persistentData.getPlayersFaction(player.getUniqueId());
						ArrayList<String> enemies = faction.getEnemyFactions();
						return filterStartingWith(args[1], tackOnBeginningAndEndQuotes(enemies));
					}
				}
				return null;
			}

			if (args.length == 4) {
				if (args[0].equalsIgnoreCase("force")) {
					if (args[1].equalsIgnoreCase("peace")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("join")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(factionNames));
					}
					if (args[1].equalsIgnoreCase("transfer")) {
						persistentData.getFactions().forEach(faction1 -> factionNames.add(faction1.getName()));
						return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(getOnlinePlayers(args[2])));
					}
					if (args[1].equalsIgnoreCase("removevassal")) {
						if(persistentData.getFaction(removeBeginningAndEndQuotes(args[2])) != null) {
							Faction faction = persistentData.getFaction(removeBeginningAndEndQuotes(args[2]));
							return filterStartingWith(args[3], tackOnBeginningAndEndQuotes(faction.getVassals()));
						}
					}
					if (args[1].equalsIgnoreCase("flag")) {
						if (persistentData.getFaction(removeBeginningAndEndQuotes(args[2])) != null) {
							Faction faction = persistentData.getFaction(removeBeginningAndEndQuotes(args[2]));
							return filterStartingWith(args[3], faction.getFlags().getFlagNamesList());
						}
					}
				}
				return null;
			}
		}

		return null;
	}
}
