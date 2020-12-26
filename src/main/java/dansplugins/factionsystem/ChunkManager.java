package dansplugins.factionsystem;

import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.*;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.entity.Player;

import java.time.ZonedDateTime;
import java.util.*;

import static java.awt.SystemColor.info;
import static org.bukkit.Bukkit.getServer;

import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.*;

public class ChunkManager {

    private static ChunkManager instance;

    // Dynmap integration related members
    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();
    private Map<String, Marker> resmark = new HashMap<String, Marker>();
    enum direction { XPLUS, ZPLUS, XMINUS, ZMINUS };
    private Plugin dynmap;
    private DynmapCommonAPI dynmapAPI;
    private MarkerAPI markerAPI;
    MarkerSet set;

    // private Map<String, AreaMarker> areaMarkers = new HashMap<String, AreaMarker>();

//    public Map<String, AreaMarker> getDynmapAreaMarkers() {
//        return areaMarkers;
//    }
//
//    public void clearDynmapAreaMarkers() {
//        areaMarkers.clear();
//    }

//    public void updateDynmapAreaMarkers(String worldName) {
//        System.out.println("Updating Dynmap Area Markers");
//        //clearDynmapAreaMarkers();
//        ArrayList<int[]> linelist = new ArrayList<int[]>();
//        int csize = 16;
//        ArrayList<ClaimedChunk> claimedChunks = PersistentData.getInstance().claimedChunks;
//        for (ClaimedChunk chunk : claimedChunks) {
//            if (chunk.getWorld().equalsIgnoreCase(worldName)) {
//                int cx = chunk.getChunk().getX() * csize;
//                int cz = chunk.getChunk().getZ() * csize;
//                linelist.add(new int[]{cx, cz}); // Add top left point
//                linelist.add(new int[]{cx + csize, cz}); // Add top right point
//                linelist.add(new int[]{cx + csize, cz + csize}); // Add bottom right point
//                linelist.add(new int[]{cx, cz + csize}); // Add bottom left point
//            }
//        }
//        dynmapSetAreaMarkerLines(worldName, linelist);
//    }


    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private int floodFillTarget(ChunkFlags src, ChunkFlags dest, int x, int y) {
        int cnt = 0;
        ArrayDeque<int[]> stack = new ArrayDeque<int[]>();
        stack.push(new int[] { x, y });

        while(stack.isEmpty() == false) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if(src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                cnt++;
                if(src.getFlag(x+1, y))
                    stack.push(new int[] { x+1, y });
                if(src.getFlag(x-1, y))
                    stack.push(new int[] { x-1, y });
                if(src.getFlag(x, y+1))
                    stack.push(new int[] { x, y+1 });
                if(src.getFlag(x, y-1))
                    stack.push(new int[] { x, y-1 });
            }
        }
        return cnt;
    }

    /* Update Faction information */
    public void dynmapUpdateFactions() {
        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<String,Marker>(); /* Build new map */

        /* Loop through factions */
        for(Faction f : PersistentData.getInstance().getFactions()) {
            dynmapUpdateFaction(f, newmap, newmark);
        }
        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : resareas.values()) {
            oldm.deleteMarker();
        }
        for(Marker oldm : resmark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        resareas = newmap;
        resmark = newmark;

    }

    private void dynmapUpdateFaction(Faction faction, Map<String, AreaMarker> newmap, Map<String, Marker> newmark) {
        String name = faction.getName();
        double[] x = null;
        double[] z = null;
        int poly_index = 0; /* Index of polygon for given town */

        /* Handle areas */
        List<ClaimedChunk> blocks = faction.getClaimedChunks();
        if(blocks.isEmpty())
            return;
        /* Build popup */
        String desc = "Info window.";
        System.out.println("Updating faction " + faction.getName() + " claims on dynmap.");
        HashMap<String, ChunkFlags> blkmaps = new HashMap<String, ChunkFlags>();
        LinkedList<ClaimedChunk> nodevals = new LinkedList<ClaimedChunk>();
        String curworld = null;
        ChunkFlags curblks = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
        for(ClaimedChunk b : blocks) {
            if(!b.getWorld().equalsIgnoreCase(curworld)) { /* Not same world */
                String wname = b.getWorld();
                curworld = b.getWorld();
                curblks = blkmaps.get(wname);
                if (curblks == null) {
                    curblks = new ChunkFlags();
                    blkmaps.put(wname, curblks);
                }
            }
            curblks.setFlag(b.getChunk().getX(), b.getChunk().getZ(), true);
            nodevals.addLast(b);
        }
        System.out.println(String.format("nodeevals = %d", nodevals.size()));
        /* Loop through until we don't find more areas */
        while(nodevals != null) {
            LinkedList<ClaimedChunk> ournodes = null;
            LinkedList<ClaimedChunk> newlist = null;
            ChunkFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(ClaimedChunk node : nodevals) {
                int nodex = node.getChunk().getX();
                int nodez = node.getChunk().getZ();
                if(ourblks == null) {   /* If not started, switch to world for this block first */
                    if(!node.getWorld().equalsIgnoreCase(curworld)) {
                        curworld = node.getWorld();
                        curblks = blkmaps.get(curworld);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
                if((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new ChunkFlags();  /* Create map for shape */
                    ournodes = new LinkedList<ClaimedChunk>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((ourblks != null) && (node.getWorld().equalsIgnoreCase(curworld)) &&
                        (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newlist == null) newlist = new LinkedList<ClaimedChunk>();
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if(ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int init_x = minx;
                int init_z = minz;
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<int[]>();
                linelist.add(new int[] { init_x, init_z } ); // Add start point
                while((cur_x != init_x) || (cur_z != init_z) || (dir != direction.ZMINUS)) {
                    switch(dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if(!ourblks.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++; cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if(!ourblks.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++; cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if(!ourblks.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--; cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if(!ourblks.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            }
                            else if(!ourblks.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }
                /* Build information for specific area */
                String polyid = faction.getName() + "__" + poly_index;
                int csize = 16;
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double)csize;
                    z[i] = (double)line[1] * (double)csize;
                }
                /* Find existing one */
                AreaMarker m = resareas.remove(polyid); /* Existing area? */
                if(m == null) {
                    m = set.createAreaMarker(polyid, name, false, curworld, x, z, false);
                    if(m == null) {
                        System.out.println("error adding area marker " + polyid);
                        return;
                    }
                    System.out.println("Added area marker " + polyid + " successfully." + String.format(" (%d line segments found)", sz));
                }
                else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(name);   /* Update label */
                    System.out.println("Updated area marker " + polyid + " successfully." + String.format(" (%d line segments found)", sz));
                }
                m.setDescription(desc); /* Set popup */

                /* Set line and fill properties */
//                String nation = NATION_NONE;
//                try {
//                    if(town.getNation() != null)
//                        nation = town.getNation().getName();
//                } catch (Exception ex) {}
//                addStyle(town.getName(), nation, m, btype);

                /* Add to map */
                newmap.put(polyid, m);
                poly_index++;
            }
        }
    }

    /***
     * Update Chunk marker on dynmap.
     */
    public void dynmapSetAreaMarkerLines(String worldName, ArrayList<int[]> lines) {
        try
        {
            AreaMarker m;
            //Map<String, AreaMarker> markers = getDynmapAreaMarkers();
//            if (markers.containsKey(polyid)) {
//                m = markers.get(polyid);
//                // TODO: update if exists already
//            }
//            else
//            {
            MarkerSet set = getMarkerAPI().getMarkerSet(getDynmapPluginSetId());
            double[] x = new double[lines.size()];
            double[] z = new double[lines.size()];
            for (int i=0; i < lines.size(); i++) {
                int[] line = lines.get(i);
                x[i] = line[0];
                z[i] = line[1];
            }
            String polyid = getDynmapChunkPolyId(worldName, 0, 0);
            set.createAreaMarker(polyid, worldName + " Claims", false, worldName, x, z, false);
        } catch (Exception e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    /***
     * Dynmap marker set id (prefix used for other ids/layer ids)
     * @return
     */
    public String getDynmapPluginSetId() { return "mf.faction"; }

    /***
     * @return Dynmap set Id for faction.
     */
    public String getDynmapFactionSetId(String holder) {
        return getDynmapPluginSetId() + "." + holder;
    }

    /***
     * @return Dynmap layer Id for faction.
     */
    public String getDynmapPluginLayer() {
        return getDynmapPluginSetId() + "_layer";
    }

    /***
     * @return Dynmap polygon Id corresponding to these chunk
     * coordinates.
     */
    public String getDynmapChunkPolyId(String worldName, int x, int z) {
        // return getDynmapFactionSetId() + "_" + String.format("%d-%d", chunk.getX(), chunk.getZ());
        return getDynmapPluginSetId() + "_" + String.format("%d-%d", x, z);
    }

    /***
     *
     * Refreshes the Dynmap Player List for the nation that owns the current chunk.
     */
    public void dynmapUpdateNationPlayerLists(String holder) {
        try {
            String setid = getDynmapFactionSetId(holder);
            MarkerAPI markerapi = getMarkerAPI();
            Set<String> plids = new HashSet<String>();
            Faction f = PersistentData.getInstance().getFaction(holder);
            if (f != null) {
                for (PlayerPowerRecord powerRecord : PersistentData.getInstance().getPlayerPowerRecords()) {
                    Faction pf = PersistentData.getInstance().getPlayersFaction(powerRecord.getPlayerUUID());
                    if (pf != null && pf.getName().equalsIgnoreCase(holder)) {
                        plids.add(UUIDChecker.getInstance().findPlayerNameBasedOnUUID(powerRecord.getPlayerUUID()));
                    }
                }
            }
            PlayerSet set = markerapi.getPlayerSet(setid);  /* See if set exists */
            if (set == null) {
                markerapi.createPlayerSet(setid, true, plids, false);
            }
            else {
                set.setPlayers(plids);
            }
        } catch (Exception e) {

        }
    }

    private ChunkManager() {
        PluginManager pm = getServer().getPluginManager();
        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");
        if(dynmap == null) {
            System.out.println("Cannot find dynmap!");
        }
        else {
            try {
                dynmapAPI = (DynmapCommonAPI) dynmap; /* Get API */

                markerAPI = dynmapAPI.getMarkerAPI();
                set = markerAPI.getMarkerSet(getDynmapPluginSetId());
                if (set == null) {
                    set = markerAPI.createMarkerSet(getDynmapPluginSetId(), getDynmapPluginLayer(), null, false);
                    if (set == null) {
                        System.out.println("Error creating marker set!");
                        return;
                    }
                } else {
                    set.setMarkerSetLabel(getDynmapPluginLayer());
                }
                System.out.println("Dynmap integration successful!");
            }
            catch (Exception e) {
                System.out.println("Error integrating with dynmap: " + e.getMessage());
            }
        }
    }

    public static ChunkManager getInstance() {
        if (instance == null) {
            instance = new ChunkManager();
        }
        return instance;
    }

    public MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    public void addChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {

                // check if land is already claimed
                ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
                if (chunk != null)
                {
                    if (playerCoords[0] == chunk.getCoordinates()[0] && playerCoords[1] == chunk.getCoordinates()[1]) {

                        // if holder is player's faction
                        if (chunk.getHolder().equalsIgnoreCase(faction.getName()) && !PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getAutoClaimStatus()) {
                            player.sendMessage(ChatColor.RED + "This land is already claimed by your faction!");
                            return;
                        }
                        else {

                            // check if faction has more land than their demesne limit
                            for (Faction targetFaction : PersistentData.getInstance().getFactions()) {
                                if (chunk.getHolder().equalsIgnoreCase(targetFaction.getName())) {
                                    if (targetFaction.getCumulativePowerLevel() < getChunksClaimedByFaction(targetFaction.getName(), PersistentData.getInstance().getClaimedChunks())) {

                                        // is at war with target faction
                                        if (faction.isEnemy(targetFaction.getName()) || everyPlayerInFactionExperiencingPowerDecay(targetFaction)) {

                                            if (MedievalFactions.getInstance().getConfig().getBoolean("surroundedChunksProtected")) {
                                                if (isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                                                    player.sendMessage(ChatColor.RED + "Target faction has claimed the chunks to the north, east, south and west of this chunk! It cannot be conquered!");
                                                    return;
                                                }
                                            }

                                            // CONQUERABLE

                                            // remove locks on this chunk
                                            Iterator<LockedBlock> itr = PersistentData.getInstance().getLockedBlocks().iterator();
                                            while (itr.hasNext()) {
                                                LockedBlock block = itr.next();
                                                if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                                                        chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ()) {
                                                    itr.remove();
                                                }
                                            }

                                            PersistentData.getInstance().getClaimedChunks().remove(chunk);

                                            ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                                            newChunk.setHolder(faction.getName());
                                            newChunk.setWorld(player.getLocation().getWorld().getName());
                                            PersistentData.getInstance().getClaimedChunks().add(newChunk);
                                            player.sendMessage(ChatColor.GREEN + "Land conquered from " + targetFaction.getName() + "! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()) + "/" + faction.getCumulativePowerLevel());

                                            Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getName() + " has conquered land from your faction!");

                                            return;
                                        }
                                        else {
                                            if (MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")) {
                                                player.sendMessage(ChatColor.RED + "In order for you to conquer land, this faction must be at war with you or every member must be experiencing power decay.");
                                            } else {
                                                player.sendMessage(ChatColor.RED + "Your factions have to be at war in order for you to conquer land.");
                                            }
                                            return;
                                        }
                                    }
                                }
                            }

                            if (!PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getAutoClaimStatus()) {
                                player.sendMessage(ChatColor.RED + "This land is already claimed by " + chunk.getHolder());
                            }

                            return;
                        }
                    }
                }

                ClaimedChunk newChunk = new ClaimedChunk(player.getLocation().getChunk());
                newChunk.setHolder(faction.getName());
                newChunk.setWorld(player.getLocation().getWorld().getName());
                PersistentData.getInstance().getClaimedChunks().add(newChunk);
                player.sendMessage(ChatColor.GREEN + "Land claimed! Demesne Size: " + getChunksClaimedByFaction(faction.getName(), PersistentData.getInstance().getClaimedChunks()) + "/" + faction.getCumulativePowerLevel());
                return;
            }
        }
        player.sendMessage(ChatColor.RED + "You must be an officer or owner of a faction to claim land!");
    }

    private ClaimedChunk isChunkClaimed(double x, double y, String world)
    {
        for (ClaimedChunk chunk : PersistentData.getInstance().getClaimedChunks())
        {
            if (x == chunk.getCoordinates()[0] && y == chunk.getCoordinates()[1] && world.equalsIgnoreCase(chunk.getWorld()))
            {
                return chunk;
            }
        }

        return null;
    }

    private boolean everyPlayerInFactionExperiencingPowerDecay(Faction faction) {
        int numExperiencingPowerDecay = 0;
        for (UUID uuid : faction.getMemberArrayList()) {
            PlayerActivityRecord record = PersistentData.getInstance().getPlayerActivityRecord(uuid);
            if (record != null) {
                Player player = getServer().getPlayer(record.getPlayerUUID());
                boolean isOnline = false;
                if (player != null)
                {
                    isOnline = player.isOnline();
                }
                if (!isOnline && MedievalFactions.getInstance().getConfig().getBoolean("powerDecreases")
                        && record.getMinutesSinceLastLogout() > MedievalFactions.getInstance().getConfig().getInt("minutesBeforePowerDecrease")) {
                    numExperiencingPowerDecay++;
                }
            }
            else {
                PlayerActivityRecord newRecord = new PlayerActivityRecord(uuid, 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
            }
        }
        return (numExperiencingPowerDecay == faction.getMemberArrayList().size());
    }

    public void removeChunkAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();

        if (EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
            ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
            if (chunk != null)
            {
                removeChunk(chunk, player, PersistentData.getInstance().getFaction(chunk.getHolder()));
                player.sendMessage(ChatColor.GREEN + "Land unclaimed using admin bypass!");
                return;
            }
            player.sendMessage(ChatColor.RED + "This land is not currently claimed!");
            return;
        }

        for (Faction faction : PersistentData.getInstance().getFactions()) {
            if (faction.isOwner(player.getUniqueId()) || faction.isOfficer(player.getUniqueId())) {
                // check if land is claimed by player's faction
                ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
                if (chunk != null)
                {
                    // if holder is player's faction
                    if (chunk.getHolder().equalsIgnoreCase(faction.getName())) {
                        removeChunk(chunk, player, faction);
                        player.sendMessage(ChatColor.GREEN + "Land unclaimed.");

                        return;
                    }
                    else {
                        player.sendMessage(ChatColor.RED + "This land is claimed by " + chunk.getHolder());
                        return;
                    }
                }

            }
        }
    }

    private void removeChunk(ClaimedChunk chunk, Player player, Faction faction) {
        String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

        // if faction home is located on this chunk
        Location factionHome = PersistentData.getInstance().getPlayersFaction(player.getUniqueId()).getFactionHome();
        if (factionHome != null) {
            if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()
                    && chunk.getWorld().equalsIgnoreCase(player.getLocation().getWorld().getName())) {
                // remove faction home
                faction.setFactionHome(null);
                Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + "Your faction home has been removed!");

            }
        }

        // remove locks on this chunk
        Iterator<LockedBlock> itr = PersistentData.getInstance().getLockedBlocks().iterator();
        while (itr.hasNext()) {
            LockedBlock block = itr.next();
            if (chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getX() == chunk.getChunk().getX() &&
                    chunk.getChunk().getWorld().getBlockAt(block.getX(), block.getY(), block.getZ()).getChunk().getZ() == chunk.getChunk().getZ() &&
                    block.getWorld().equalsIgnoreCase(chunk.getWorld())) {
                itr.remove();
            }
        }

        // remove any gates in this chunk
        Iterator<Gate> gtr = faction.getGates().iterator();
        while(gtr.hasNext())
        {
            Gate gate = gtr.next();
            if (isGateInChunk(gate, chunk))
            {
//        		System.out.println("Removing gate " + gate.getName());
                faction.removeGate(gate);
                gtr.remove();
            }
        }

        PersistentData.getInstance().getClaimedChunks().remove(chunk);
    }

    public String checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
        if (chunk != null)
        {
            return chunk.getHolder();
        }
        return "unclaimed";
    }

    public boolean isGateInChunk(Gate gate, ClaimedChunk chunk)
    {
        if ((gate.getTopLeftChunkX() == chunk.getCoordinates()[0] || gate.getBottomRightChunkX() == chunk.getCoordinates()[0])
                && (gate.getTopLeftChunkZ() == chunk.getCoordinates()[1] || gate.getBottomRightChunkZ() == chunk.getCoordinates()[1]))
        {
            return true;
        }
        return false;
    }

    public int getChunksClaimedByFaction(String factionName, ArrayList<ClaimedChunk> claimedChunks) {
        int counter = 0;
        for (ClaimedChunk chunk : claimedChunks) {
            if (chunk.getHolder().equalsIgnoreCase(factionName)) {
                counter++;
            }
        }
        return counter;
    }

    public ClaimedChunk getClaimedChunk(int x, int z, String world, ArrayList<ClaimedChunk> claimedChunks) {
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == x && claimedChunk.getCoordinates()[1] == z && claimedChunk.getWorld().equalsIgnoreCase(world)) {
                return claimedChunk;
            }
        }
        return null;
    }

    private Chunk getChunkByDirection(Chunk origin, String direction) {

        int xpos = -1;
        int zpos = -1;

        if (direction.equalsIgnoreCase("north")) {
            xpos = origin.getX();
            zpos = origin.getZ() + 1;
        }
        if (direction.equalsIgnoreCase("east")) {
            xpos = origin.getX() + 1;
            zpos = origin.getZ();
        }
        if (direction.equalsIgnoreCase("south")) {
            xpos = origin.getX();
            zpos = origin.getZ() - 1;
        }
        if (direction.equalsIgnoreCase("west")) {
            xpos = origin.getX() - 1;
            zpos = origin.getZ();
        }

        return origin.getWorld().getChunkAt(xpos, zpos);
    }

    private ClaimedChunk getClaimedChunk(Chunk chunk) {
        return getClaimedChunk(chunk.getX(), chunk.getZ(), chunk.getWorld().getName(), PersistentData.getInstance().getClaimedChunks());
    }

    // this will return true if the chunks to the North, East, South and West of the target are claimed by the same faction as the target
    private boolean isClaimedChunkSurroundedByChunksClaimedBySameFaction(ClaimedChunk target) {
        ClaimedChunk northernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "north"));
        ClaimedChunk easternClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "east"));
        ClaimedChunk southernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "south"));
        ClaimedChunk westernClaimedChunk = getClaimedChunk(getChunkByDirection(target.getChunk(), "west"));

        if (northernClaimedChunk == null ||
                easternClaimedChunk == null ||
                southernClaimedChunk == null ||
                westernClaimedChunk == null) {

            return false;

        }

        boolean northernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(northernClaimedChunk.getHolder());
        boolean easternChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(easternClaimedChunk.getHolder());
        boolean southernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(southernClaimedChunk.getHolder());
        boolean westernChunkClaimedBySameFaction = target.getHolder().equalsIgnoreCase(westernClaimedChunk.getHolder());

        return (northernChunkClaimedBySameFaction &&
                easternChunkClaimedBySameFaction &&
                southernChunkClaimedBySameFaction &&
                westernChunkClaimedBySameFaction);
    }

    public boolean isClaimed(Chunk chunk, ArrayList<ClaimedChunk> claimedChunks) {

        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ() && claimedChunk.getWorld().equalsIgnoreCase(chunk.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    public void removeAllClaimedChunks(String factionName, ArrayList<ClaimedChunk> claimedChunks) {

        Iterator<ClaimedChunk> itr = claimedChunks.iterator();

        while (itr.hasNext()) {
            ClaimedChunk currentChunk = itr.next();
            if (currentChunk.getHolder().equalsIgnoreCase(factionName)) {
                try {
                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println("An error has occurred during claimed chunk removal.");
                }
            }
        }
    }

    public boolean isFactionExceedingTheirDemesneLimit(Faction faction, ArrayList<ClaimedChunk> claimedChunks) {
        return (getChunksClaimedByFaction(faction.getName(), claimedChunks) > faction.getCumulativePowerLevel());
    }

    public void informPlayerIfTheirLandIsInDanger(Player player, ArrayList<Faction> factions, ArrayList<ClaimedChunk> claimedChunks) {
        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction, claimedChunks)) {
                player.sendMessage(ChatColor.RED + "Your faction has more claimed chunks than power! Your land can be conquered!");
            }
        }
    }

}
