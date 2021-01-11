package dansplugins.factionsystem.commands;

import dansplugins.factionsystem.LocaleManager;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.objects.Duel;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

public class DuelCommand {
	
	public void handleDuel(CommandSender sender, String[] args) {
		if (sender instanceof Player)
		{
			Player player = (Player) sender;
			
			if (player.hasPermission("mf.duel"))
			{
				if (args.length > 1)
				{
					if (args[1].equalsIgnoreCase("challenge"))
					{
						if (args.length > 2)
						{
							if (args[2].equalsIgnoreCase(player.getName()))
							{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotDuelSelf"));
								return;
							}
							if (isDuelling(player))
							{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDuelingSomeone"));
								return;
							}
							Player target = Bukkit.getServer().getPlayer(args[2]);
							if (target != null)
							{
								if (!isDuelling(target))
								{
									int timeLimit = 120; // Time limit in seconds. TODO: Make config option.
									if (args.length == 4)
									{
										timeLimit = Integer.parseInt(args[3]);
									}
									inviteDuel(player, target, timeLimit);
									player.sendMessage(ChatColor.AQUA + "" + target.getName() + LocaleManager.getInstance().getText("AlertDuelChallengeReceived"));
									return;
								}
								else
								{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerAlreadyDueling"));
									return;
								}
							}
							else
							{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("PlayerNotFound"));
								return;
							}
						}
					}
					else if (args[1].equalsIgnoreCase("accept"))
					{
						if (isDuelling(player))
						{
							player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDuelingSomeone"));
							return;
						}
						// If a name is specified to accept the challenge from, look for that specific name.
						if (args.length > 2)
						{
		                	Player challenger = Bukkit.getServer().getPlayer(args[2]);
		                	Duel duel = EphemeralData.getInstance().getDuel(challenger, player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDuelingPlayer"));
									return;
		                		}
		                		if (duel.isChallenged(player))
		                		{
		                			duel.acceptDuel();
		                		}
		                		else
		                		{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallengedByPlayer"));
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallengedByPlayer"));
								return;
		                	}
						}
						else
						{
		                	Duel duel = getDuel(player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertAlreadyDueling"));
									return;
		                		}
		                		if (duel.isChallenged(player))
		                		{
		                			duel.acceptDuel();
		                		}
		                		else
		                		{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallenged"));
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("AlertNotBeenChallenged"));
								return;
		                	}
						}
					}
					else if (args[1].equalsIgnoreCase("cancel"))
					{
		                if (isDuelling(player))
		                {
		                	Duel duel = getDuel(player);
		                	if (duel != null)
		                	{
		                		if (duel.getStatus().equals(Duel.DuelState.DUELLING))
		                		{
									player.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("CannotCancelActiveDuel"));
									return;
		                		}
		                		else
		                		{
		                			EphemeralData.getInstance().getDuelingPlayers().remove(duel);
									player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("DuelChallengeCancelled"));
									return;
		                		}
		                	}
		                	else
		                	{
								player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertNoPendingChallenges"));
								return;
		                	}
		                }
	                	else
	                	{
							player.sendMessage(ChatColor.AQUA + LocaleManager.getInstance().getText("AlertNoPendingChallenges"));
							return;
	                	}

					}
					else
					{
						sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
						sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelChallenge"));
						sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelAccept"));
						sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelCancel"));
					}
				}
				else
				{
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("SubCommands"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelChallenge"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelAccept"));
			        sender.sendMessage(ChatColor.RED + LocaleManager.getInstance().getText("HelpDuelCancel"));
				}
			}
		}
	}

	private Duel getDuel(Player player)
	{
		for (Duel duel : EphemeralData.getInstance().getDuelingPlayers())
		{
			if (duel.isChallenged(player) || duel.isChallenger(player))
			{
				return duel;
			}
		}
		return null;
	}

	private boolean isDuelling(Player player)
	{
		for (Duel duel : EphemeralData.getInstance().getDuelingPlayers())
		{
			if (duel.hasPlayer(player) && duel.getStatus().equals(Duel.DuelState.DUELLING))
			{
				return true;
			}
		}
		return false;
	}

	private void inviteDuel(Player player, Player target, int limit)
	{
		target.sendMessage(ChatColor.AQUA + player.getName() + LocaleManager.getInstance().getText("AlertChallengedToDuelPlusHowTo"));
		EphemeralData.getInstance().getDuelingPlayers().add(new Duel(player, target, limit));
	}
}
