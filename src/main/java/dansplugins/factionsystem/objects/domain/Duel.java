package dansplugins.factionsystem.objects.domain;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.objects.domain.specification.IDuel;
import net.md_5.bungee.api.ChatColor;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.OfflinePlayer;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.ArrayList;

public class Duel implements IDuel {

	private DuelState duelState = DuelState.INVITED;

	@Override
	public DuelState getStatus()
	{
		return duelState;
	}

	@Override
	public void setStatus(DuelState state)
	{
		duelState = state;
	}
	
	private BossBar bar = null;
	Player _challenged = null;
	double challengedHealth = 0;
	Player _challenger = null;
	double challengerHealth = 0;
	Player winner = null;
	Player loser = null;
	int repeatingTaskId = 0;
	
	float nearbyPlayerRadius = 64;
	double timeLimit = 120.0;
	double timeDecrementAmount = 0;

	@Override
	public boolean isChallenged(Player player)
	{
		return player.equals(_challenged);
	}

	@Override
	public Player getChallenged()
	{
		return _challenged;
	}

	@Override
	public boolean isChallenger(Player player)
	{
		return player.equals(_challenger);
	}

	@Override
	public Player getChallenger()
	{
		return _challenger;
	}

	@Override
	public double getChallengerHealth()
	{
		return challengerHealth;
	}

	@Override
	public double getChallengedHealth()
	{
		return challengedHealth;
	}

	@Override
	public boolean hasPlayer(Player player)
	{
		if (_challenged.equals(player) || _challenger.equals(player))
		{
			return true;
		}
		return false;
	}
	@Override

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

	@Override
	public void setWinner(Player player)
	{
		duelState = DuelState.WINNER;
		winner = player;
		if (isChallenger(player))
		{
			loser = getChallenged();
		}
		else
		{
			loser = getChallenger();
		}
	}

	@Override
	public Player getWinner()
	{
		return winner;
	}

	@Override
	public void setLoser(Player player)
	{		
		duelState = DuelState.WINNER;
		loser = player;
		if (isChallenger(player))
		{
			winner = getChallenged();
		}
		else
		{
			winner = getChallenger();
		}
	}

	@Override
	public Player getLoser()
	{
		return loser;
	}

	@Override
	public void acceptDuel()
	{
		// Participants that the challenged was accepted and that it's game-on.
		getChallenger().sendMessage(String.format(ChatColor.AQUA + "%s has accepted your challenge, the duel has begun!", _challenged.getName()));
		getChallenged().sendMessage(String.format(ChatColor.AQUA + "You have accepted %s's challenge, the duel has begun!", _challenger.getName()));
		
		challengerHealth = _challenger.getHealth(); 
		challengedHealth = _challenged.getHealth();
		duelState = DuelState.DUELLING;
		// Announce to nearby players that a duel has started.
		for (Player other : MedievalFactions.getInstance().getServer().getOnlinePlayers()) {
			if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
					other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
				other.sendMessage(String.format(ChatColor.AQUA + "%s has challenged %s to a duel!", _challenger.getName(), _challenged.getName()));
			}
		}
		
		bar = MedievalFactions.getInstance().getServer().createBossBar(String.format(ChatColor.AQUA + "%s vs %s", _challenger.getName(), _challenged.getName())
				, BarColor.WHITE, BarStyle.SEGMENTED_20);
		bar.setProgress(1);
		timeDecrementAmount = 1.0 / timeLimit;
		bar.addPlayer(_challenger);
		bar.addPlayer(_challenged);
		
    	repeatingTaskId = Bukkit.getScheduler().scheduleSyncRepeatingTask(MedievalFactions.getInstance(), new Runnable () {
    		@Override
    		public void run() {
    			double progress = bar.getProgress() - timeDecrementAmount;
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

	private ItemStack getHead(Player player) {
		ItemStack item = new ItemStack(Material.PLAYER_HEAD, 1);
		SkullMeta skull = (SkullMeta) item.getItemMeta();
		skull.setDisplayName(player.getName() + "'s head.");
		OfflinePlayer offlinePlayer = (OfflinePlayer) getLoser();
		skull.setOwningPlayer(offlinePlayer);
		ArrayList<String> lore = new ArrayList<String>();
		lore.add("Lost in a duel against " + getWinner().getPlayer().getName() + ".");
		skull.setLore(lore);
		item.setItemMeta(skull);
		return item;
	}

	@Override
	public void finishDuel(boolean tied)
	{
		_challenger.setHealth(challengerHealth);
		_challenged.setHealth(challengedHealth);

		// Remove player damaging effects like fire or poison before ending the duel.
		_challenged.getActivePotionEffects().clear();
		_challenger.getActivePotionEffects().clear();

		if (!tied)
		{
			// Announce winner to nearby players.
			for (Player other : MedievalFactions.getInstance().getServer().getOnlinePlayers()) {
				if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
						other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
					other.sendMessage(String.format(ChatColor.AQUA + "%s has defeated %s in a duel!", winner.getName(), loser.getName()));
				}
			}
			if (getWinner().getInventory().firstEmpty() > -1)
			{
				getWinner().getInventory().addItem(getHead(getLoser()));
			}
			else
			{
				getWinner().getWorld().dropItemNaturally(getWinner().getLocation(), getHead(getLoser()));
			}
		}
		else
		{
    		for (Player other : MedievalFactions.getInstance().getServer().getOnlinePlayers()) {
    			if (other.getLocation().distance(_challenger.getLocation()) <= nearbyPlayerRadius ||
    					other.getLocation().distance(_challenged.getLocation()) <= nearbyPlayerRadius) {
    				other.sendMessage(String.format(ChatColor.YELLOW + "%s and %s's duel has ended in a tie.", _challenger.getName(), _challenged.getName()));
    			}
    		}
		}
		bar.removeAll();
		MedievalFactions.getInstance().getServer().getScheduler().cancelTask(repeatingTaskId);
    	EphemeralData.getInstance().getDuelingPlayers().remove(this);
	}
	
	public Duel(Player challenger, Player challenged, int limit)
	{
		_challenger = challenger;
		challengerHealth = challenger.getHealth(); 
		_challenged = challenged;
		challengedHealth = challenged.getHealth();
		timeLimit = limit;
		duelState = DuelState.INVITED;
	}
	
	
	
}
