package factionsystem.Objects;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;

import factionsystem.Main;

public class Gate {

	private String name = "gateName";
	private boolean open = false;
	private boolean vertical = true;
	private GateCoord coord1 = null;
	private GateCoord coord2 = null;
	private GateCoord trigger = null;
	private Material material = Material.IRON_BARS;
	private World world = null;
	
	private Sound soundEffect = Sound.BLOCK_ANVIL_HIT;
	
	public boolean isOpen()
	{
		return open ? true : false;
	}
	
	public boolean isClosed()
	{
		return open ? false : true;
	}
	
	public GateCoord getTrigger()
	{
		return trigger;
	}
	
	public GateCoord getCoord1()
	{
		return coord1;
	}
	
	public GateCoord getCoord2()
	{
		return coord2;
	}
	
	private Main main;
	
	public Gate(Main plugin)
	{
		main = plugin;
	}
	
	public boolean AddCoord(Block clickedBlock)
	{
		if (coord1 == null)
		{
			world = clickedBlock.getWorld();
			coord1 = new GateCoord(clickedBlock);
			System.out.println("coord1 =" + clickedBlock.toString());
		}
		else if (coord2 == null)
		{
			System.out.println("coord2 = " + clickedBlock.toString());
			if (!coord1.getWorld().equalsIgnoreCase(clickedBlock.getWorld().getName()))
			{
				return false;
			}
			// TODO: Check to see if it's within config area setting
			// TODO: Check to see if it's vertical or horizontal.
			// if it's neither, reject (we can't allow a cuboid).
			coord2 = new GateCoord(clickedBlock);
		}
		else
		{
			System.out.println("trigger = " + clickedBlock.toString());
			trigger = new GateCoord(clickedBlock);
		}
		return true;
	}
	
	public void OpenGate()
	{
		if (open)
			return;
		
		open = true;
		// For vertical we only need to iterate over x/y
		if (vertical)
		{
			int topY = coord1.getY();
			int bottomY = coord2.getY();
			
			int _leftX = coord1.getX();
			int _rightX = coord2.getX();

			final int leftX = _leftX;
			final int rightX = _rightX;
			
			int c = 0;
			for (int y = bottomY; y <= topY; y++)
			{
				c++;
				final int blockY = y;
				Bukkit.getScheduler().runTaskLater(main, new Runnable() {
					Block b;
                    @Override
                    public void run() {
        				for (int x = leftX; x <= rightX; x++)
        				{
        					b = world.getBlockAt(x, blockY, coord1.getZ());
        					b.setType(Material.AIR);
        					world.playSound(b.getLocation(), soundEffect, 0.1f, 0.1f);
        				}
                    }
                }, c * 10);
			}
			
		}
		else
		{
			// TODO: Bridge code iterates over x/z
		}
	}
	
	public void CloseGate()
	{
		if (!open)
			return;
		
		open = false;
		// For vertical we only need to iterate over x/y
		if (vertical)
		{
			int topY = coord1.getY();
			int bottomY = coord2.getY();
			if (coord2.getY() > coord1.getY())
			{
				topY = coord2.getY();
				bottomY = coord1.getY();
			}
			
			int _leftX = coord1.getX();
			int _rightX = coord2.getX();
			if (coord2.getX() < coord1.getX())
			{
				_leftX = coord2.getX();
				_rightX = coord1.getX();
			}
			final int leftX = _leftX;
			final int rightX = _rightX;
			
			
			int c = 0;
			for (int y = topY; y >= bottomY; y--)
			{
				c++;
				final int blockY = y;
				Bukkit.getScheduler().runTaskLater(main, new Runnable() {
					Block b;
                    @Override
                    public void run() {
        				for (int x = leftX; x <= rightX; x++)
        				{
        					b = world.getBlockAt(x, blockY, coord1.getZ());
        					b.setType(material);
        					world.playSound(b.getLocation(), soundEffect, 0.1f, 1.1f);
        				}
                    }
                }, c * 10);
			}
			
		}
		else
		{
			// TODO: Bridge code iterates over x/z
		}
	}
}
