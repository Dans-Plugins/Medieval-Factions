package factionsystem.Objects;

import org.bukkit.block.Block;

public class GateCoord {
	
	private int x;
	private int y;
	private int z;
	private String world;
	
	public String getWorld()
	{
		return world;
	}
	
	public int getX()
	{
		return x;
	}
	public int getY()
	{
		return y;
	}
	public int getZ()
	{
		return z;
	}
	
	public GateCoord(int X, int Y, int Z, String World)
	{
		x = X;
		y = Y;
		z = Z;
		world = World;
	}
	
	public GateCoord(Block block)
	{
		x = block.getX();
		y = block.getY();
		z = block.getZ();
		world = block.getWorld().getName();
	}
	
	public boolean equals(Block block)
	{
		return block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().getName().equalsIgnoreCase(world);
	}
		
}
