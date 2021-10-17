package dansplugins.factionsystem.integrators;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.ChunkManager;
import dansplugins.factionsystem.managers.LocaleManager;
import dansplugins.factionsystem.objects.ChunkFlags;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PlayerPowerRecord;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.factionsystem.utils.UUIDChecker;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.*;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

public class DynmapIntegrator {

    private static DynmapIntegrator instance = null;
    public static boolean dynmapInitialized = false;

    public boolean updateClaimsAreaMarkers = false;

    public static boolean hasDynmap() {
        if (!dynmapInitialized) {
            if (getInstance() == null) {
                return false;
            }
            else {
                return true;
            }
        }
        return true;
    }

    public static DynmapIntegrator getInstance() {
        if (instance == null) {
            instance = new DynmapIntegrator();
        }
        dynmapInitialized = true;
        return instance;
    }

    /***
     * Scheduled task that checks to see if there are changes to the claims that need
     * to be rendered on dynmap.
     * @param interval Number of ticks before the scheduled task executes again.
     */
    public void scheduleClaimsUpdate(long interval) {
        new BukkitRunnable() {
            @Override
            public void run() {
                if (!isDynmapPresent()) {
                    return;
                }
                if (DynmapIntegrator.getInstance().updateClaimsAreaMarkers) {
                    if (realms != null) {
                        realms.deleteMarkerSet();
                        claims.deleteMarkerSet();
                    }
                    initializeMarkerSets();
                    DynmapIntegrator.getInstance().dynmapUpdateFactions();
                    DynmapIntegrator.getInstance().dynmapUpdateRealms();
                    DynmapIntegrator.getInstance().updateClaimsAreaMarkers = false;
                }
            }
        }.runTaskTimer(MedievalFactions.getInstance(), 40, interval);
    }

    /***
     * Tell the scheduled task that we have made changes and it should update the
     * area markers.
     */
    public void updateClaims() {
        if (!isDynmapPresent()) {
            return;
        }

        if (DynmapIntegrator.hasDynmap()) {
            DynmapIntegrator.getInstance().updateClaimsAreaMarkers = true;
        }
    }

    // Dynmap integration related members

    // Claims/factions markers
    private Map<String, AreaMarker> resareas = new HashMap<String, AreaMarker>();
    private Map<String, Marker> resmark = new HashMap<String, Marker>();

    // Realms markers
    private Map<String, AreaMarker> realmsareas = new HashMap<String, AreaMarker>();
    private Map<String, Marker> realmsmark = new HashMap<String, Marker>();

    enum direction { XPLUS, ZPLUS, XMINUS, ZMINUS };
    private Plugin dynmap;
    private DynmapCommonAPI dynmapAPI;
    private MarkerAPI markerAPI;
    MarkerSet claims;
    MarkerSet realms;

    public DynmapIntegrator() {
        PluginManager pm = getServer().getPluginManager();

        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");

        if(!isDynmapPresent()) {
            Logger.getInstance().log(LocaleManager.getInstance().getText("CannotFindDynmap"));
        }
        else {
            try {
                dynmapAPI = (DynmapCommonAPI) dynmap; /* Get API */
                markerAPI = dynmapAPI.getMarkerAPI();
                initializeMarkerSets();
                Logger.getInstance().log(LocaleManager.getInstance().getText("DynmapIntegrationSuccessful"));
            }
            catch (Exception e) {
                Logger.getInstance().log(LocaleManager.getInstance().getText("ErrorIntegratingWithDynmap") + e.getMessage());
            }
        }
    }

    private void initializeMarkerSets() {
        claims = markerAPI.getMarkerSet(getDynmapPluginSetId("claims"));
        claims = initializeMarkerSet(claims, "Claims");

        realms = markerAPI.getMarkerSet(getDynmapPluginSetId("realms"));
        realms = initializeMarkerSet(realms, "Realms");
    }

    private MarkerSet initializeMarkerSet(MarkerSet set, String markerLabel) {
        if (set == null) {
            set = markerAPI.createMarkerSet(getDynmapPluginSetId(markerLabel), getDynmapPluginLayer(), null, false);
            if (set == null) {
                Logger.getInstance().log(LocaleManager.getInstance().getText("ErrorCreatingMarkerSet") + ": markerLabel = " + markerLabel);
                return set;
            }
        }
        set.setMarkerSetLabel(markerLabel);
        return set;
    }

    private boolean isDynmapPresent() {
        return (dynmap != null);
    }

    private MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

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

    /* Update Realm information */
    private void dynmapUpdateRealms() {
        // Realms Layer

        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<String,Marker>(); /* Build new map */

        /* Loop through realms and build area markers coloured in the same colour
            as each faction's liege's colour. */
        for(Faction f : PersistentData.getInstance().getFactions()) {
            String liegeName = f.getTopLiege();
            Faction liege = PersistentData.getInstance().getFaction(liegeName);
            String liegeColor;
            String popupText = "";
            // If there's no liege, then f is the liege.
            if (liege != null) {
                liegeColor = liege.getFlags().getFlag("dynmapTerritoryColor").toString();
                popupText = buildNationPopupText(liege);
            }
            else {
                liegeColor = f.getFlags().getFlag("dynmapTerritoryColor").toString();
                liegeName = f.getName() + "__parent";
                popupText = buildNationPopupText(f);
            }
            dynmapUpdateFaction(f, realms, newmap, "realm", liegeName + "__" + getClass().getName(), popupText, liegeColor, newmap, newmark);
        }

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : realmsareas.values()) {
            oldm.deleteMarker();
        }
        for(Marker oldm : realmsmark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        realmsareas.putAll(newmap);
        realmsmark.putAll(newmark);
    }

    /* Update Faction information */
    private void dynmapUpdateFactions() {
        // Claims Layer

        Map<String,AreaMarker> newmap = new HashMap<String,AreaMarker>(); /* Build new map */
        Map<String,Marker> newmark = new HashMap<String,Marker>(); /* Build new map */

        /* Loop through factions and build coloured faction area markers. */
        for(Faction f : PersistentData.getInstance().getFactions()) {
            dynmapUpdateFaction(f, claims, newmap, "claims", f.getName(), buildNationPopupText(f), f.getFlags().getFlag("dynmapTerritoryColor").toString(), newmap, newmark);
        }

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldm : resareas.values()) {
            oldm.deleteMarker();
        }
        for(Marker oldm : resmark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        resareas.putAll(newmap);
        resmark.putAll(newmark);
    }

    private String buildNationPopupText(Faction f) {
        String message = "<h4>" + f.getName() + "</h4>" +
                "Owner: " + UUIDChecker.getInstance().findPlayerNameBasedOnUUID(f.getOwner()) + "<br/>" +
                "Description: " + f.getDescription() + "<br/>" +
                "<div style='display: inline;' title='" + f.getMemberListSeparatedByCommas() + "'>Population: " + f.getMemberList().size() + "</div><br/>";

        if (f.hasLiege()) {
            message += "Liege: " + f.getLiege() + "<br/>";
        }
        if (f.isLiege()) {
            message += "Vassals: " + f.getVassalsSeparatedByCommas() + "<br/>";
        }
        message += "Allied With: " + f.getAlliesSeparatedByCommas() + "<br/>" +
                "At War With: " + f.getEnemiesSeparatedByCommas() + "<br/>" +
                "Power Level: " + f.getCumulativePowerLevel() + "<br/>" +
                "Demesne Size: " + String.format("%d/%d",
                                ChunkManager.getInstance().getChunksClaimedByFaction(f.getName(), PersistentData.getInstance().getClaimedChunks()),
                                                                    f.getCumulativePowerLevel());
        return message;
    }

    private void dynmapUpdateFaction(Faction faction, MarkerSet markerSet, Map<String, AreaMarker> areaMarkers, String type, String name, String popupDescription, String colorCode, Map<String, AreaMarker> newmap, Map<String, Marker> newmark) {
        double[] x = null;
        double[] z = null;
        int poly_index = 0; /* Index of polygon for given town */

        /* Handle areas */
        List<ClaimedChunk> blocks = faction.getClaimedChunks();
        if(blocks.isEmpty())
            return;
        /* Build popup */
        String desc = "Info window.";
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
                String polyid = name + "__" + type + "__" + poly_index;
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
                AreaMarker m = areaMarkers.remove(polyid); /* Existing area? */
                if(m == null) {
                    m = markerSet.createAreaMarker(polyid, name, false, curworld, x, z, false);
                    if(m == null) {
                        System.out.println(String.format(LocaleManager.getInstance().getText("ErrorAddingAreaMarker"), polyid));
                        return;
                    }
                }
                else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(name);   /* Update label */
                }
                String fillColor = colorCode;
                try
                {
                    int colrCode = Integer.decode(fillColor);
                    if (type.equalsIgnoreCase("realm")) {
                        m.setLineStyle(4, 1.0, colrCode);
                        m.setFillStyle(0.0, colrCode);
                    } else {
                        m.setLineStyle(1, 1.0, colrCode);
                        m.setFillStyle(0.3, colrCode);
                    }
                } catch (Exception e) {
                    System.out.println(String.format(LocaleManager.getInstance().getText("ErrorSettingAreaMarkerColor"), fillColor));
                }
                m.setDescription(popupDescription); /* Set popup */

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
     * Dynmap marker set id (prefix used for other ids/layer ids)
     * @return
     */
    private String getDynmapPluginSetId(String type) { return "mf.faction." + type; }

    /***
     * @return Dynmap set Id for faction.
     */
    private String getDynmapFactionSetId(String holder) {
        return getDynmapPluginSetId("holder") + "." + holder;
    }

    /***
     * @return Dynmap layer Id for faction.
     */
    private String getDynmapPluginLayer() {
        return getDynmapPluginSetId("layer") + "_layer";
    }

    /***
     * @return Dynmap polygon Id corresponding to these chunk
     * coordinates.
     */
    private String getDynmapChunkPolyId(String worldName, int x, int z) {
        // return getDynmapFactionSetId() + "_" + String.format("%d-%d", chunk.getX(), chunk.getZ());
        return getDynmapPluginSetId("poly") + "_" + String.format("%d-%d", x, z);
    }

    /***
     *
     * Refreshes the Dynmap Player List for the nation that owns the current chunk.
     */
    private void dynmapUpdateNationPlayerLists(String holder) {
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

}
