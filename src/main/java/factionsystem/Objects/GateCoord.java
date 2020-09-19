package factionsystem.Objects;

import org.bukkit.block.Block;

import factionsystem.Main;

public class GateCoord {
	
	private int x;
	private int y;
	private int z;
	private String world;
	
	public String getWorld()
	{
		return world;
	}
	
	public String toString()
	{
		return String.format("%d,%d,%d,%s", x, y, z, world);
	}
	
	public static GateCoord fromString(String data, Main main)
	{
		String parts[] = data.split(",");
		for (String part : parts)
		{
			System.out.println(part);
		}

		GateCoord coord = new GateCoord();
		coord.x = Integer.parseInt(parts[0]);
		coord.y = Integer.parseInt(parts[1]);
		coord.z = Integer.parseInt(parts[2]);
		coord.world = parts[3];
		return coord;
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
	
	// Only use this when instantiating for load from JSON
	public GateCoord()
	{
		
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
