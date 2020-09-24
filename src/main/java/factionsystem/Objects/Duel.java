package factionsystem.Objects;

import static org.bukkit.Bukkit.getServer;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BossBar;
import org.bukkit.boss.BarStyle;
import org.bukkit.entity.Player;

import factionsystem.Main;
import net.md_5.bungee.api.ChatColor;

public class Duel {

	public enum DuelState { INVITED, DUELLING, WINNER, TIED }
	private DuelState duelState = DuelState.INVITED;
	
	public DuelState getStatus()
	{
		return duelState;
	}
	
	public void setStatus(DuelState state)
	{
		duelState = state;
	}
	
	private BossBar bar = null;
	Main plugin = null;
	Player _challenged = null;
	double challengedHealth = 0;
	Player _challenger = null;
	double challengerHealth = 0;
	String winner = "";
	String loser = "";
	int repeatingTaskId = 0;
	
	float nearbyPlayerRadius = 64;
	int defaultTimeLimit = 2; // minutes. 
	
	public boolean isChallenged(Player player)
	{
		return player.equals(_challenged);
	}
	public Player getChallenged()
	{
		return _challenged;
	}
	
	public boolean isChallenger(Player player)
	{
		return player.equals(_challenger);
	}
	public Player getChallenger()
	{
		return _challenger;
	}
	
	public double getChallengerHealth()
	{
		return challengerHealth;
	}
	
	public double getChallengedHealth()
	{
		return challengedHealth;
	}
	
	public boolean hasPlayer(Player player)
	{
		if (_challenged.equals(player) || _challenger.equals(player))
		{
			return true;
		}
		return false;
	}
	
	public void resetHealth()
	{
		if (_challenger != null)
		{
			_challenger.setHealth(challengerHealth);
		}
		if (_challenged != null)
		{
			_challenged.setHealth(challengedHealth);
		}
	}
	
	public void setWinner(Player player)
	{
		duelState = DuelState.WINNER;
		winner = player.getName();
		if (isChallenger(player))
		{
			loser = getChallenged().getName();
		}
		else
		{
			loser = getChallenger().getName();
		}
	}
	public String getWinner()
	{
		return winner;
	}
	
	public void setLoser(Player player)
	{		
		duelState = DuelState.WINNER;
		loser = player.getName();
		if (isChallenger(player))
		{
			winner = getChallenged().getName();
		}
		else
		{
			winner = getChallenger().getName();
		}
	}
	public String getLoser()
	{
		return loser;
	}
	
	public void acceptDuel()
	{
		// Participants that the challenged was accepted and that it's game-on.
		getChallenger().sendMessage(String.format(ChatColor.AQUA + "%s has accepted your challenge, the duel has begun!", winner, loser));
		getChallenged().sendMessage(String.format(ChatColor.AQUA + "You have accepted %s's challenge, the duel has begun!", winner, loser));
		
		challengerHealth = _challenger.getHealth(); 
		challengedHealth = _challenged.getHealth();
		duelState = DuelState.DUELLING;
		// Announce to nearby players that a duel has started.
		for (Player other : plugin.getServer().getOnlinePlayers()) {
			if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
					other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
				other.sendMessage(String.format(ChatColor.AQUA + "%s has challenged %s to a duel!", _challenger.getName(), _challenged.getName()));
			}
		}
		
		bar = plugin.getServer().createBossBar(String.format(ChatColor.AQUA + "%s vs %s", _challenger.getName(), _challenged.getName())
				, BarColor.WHITE, BarStyle.SEGMENTED_20);
		bar.setProgress(1);
		bar.addPlayer(_challenger);
		bar.addPlayer(_challenged);
		
    	repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(plugin, new Runnable () {
    		@Override
    		public void run() {
    			double progress = bar.getProgress() - 0.01;
    			if (progress <= 0)
    			{
        			bar.setProgress(0);
    				finishDuel(true);
    			}
    			else
    			{
    				bar.setProgress(progress);
    			}
    		}
    	}, 1 * 20, 1 * 20);
	}
	
	public void finishDuel(boolean tied)
	{
		_challenger.setHealth(challengerHealth);
		_challenged.setHealth(challengedHealth);
		if (!tied)
		{
			// Announce winner to nearby players.
			for (Player other : plugin.getServer().getOnlinePlayers()) {
				if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
						other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
					other.sendMessage(String.format(ChatColor.AQUA + "%s has defeated %s in a duel!", winner, loser));
				}
			}
		}
		else
		{
    		for (Player other : plugin.getServer().getOnlinePlayers()) {
    			if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
    					other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
    				other.sendMessage(String.format(ChatColor.YELLOW + "%s and %s's duel has ended in a tie.", _challenger.getName(), _challenged.getName()));
    			}
    		}
		}
		bar.removeAll();
		plugin.getServer().getScheduler().cancelTask(repeatingTaskId);
    	plugin.duelingPlayers.remove(this);
	}
	
	public Duel(Player challenger, Player challenged, Main main)
	{
		_challenger = challenger;
		challengerHealth = challenger.getHealth(); 
		_challenged = challenged;
		challengedHealth = challenged.getHealth();
		duelState = DuelState.INVITED;
		plugin = main;
	}
	
	
	
}
