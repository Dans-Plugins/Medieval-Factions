package dansplugins.factionsystem.objects.helper;

import org.bukkit.block.Block;

public class GateCoord implements dansplugins.factionsystem.objects.helper.specification.IGateCoord {
	
	private int x;
	private int y;
	private int z;
	private String world;

	@Override
	public String getWorld()
	{
		return world;
	}

	@Override
	public String toString()
	{
		return String.format("%d,%d,%d,%s", x, y, z, world);
	}
	
	public static GateCoord fromString(String data)
	{
		String parts[] = data.split(",");
		for (String part : parts)
		{
//			System.out.println(part);
		}

		GateCoord coord = new GateCoord();
		coord.x = Integer.parseInt(parts[0]);
		coord.y = Integer.parseInt(parts[1]);
		coord.z = Integer.parseInt(parts[2]);
		coord.world = parts[3];
		return coord;
	}

	@Override
	public int getX()
	{
		return x;
	}

	@Override
	public int getY()
	{
		return y;
	}

	@Override
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

	@Override
	public boolean equals(Block block)
	{
		return block.getX() == x && block.getY() == y && block.getZ() == z && block.getWorld().getName().equalsIgnoreCase(world);
	}
		
}
