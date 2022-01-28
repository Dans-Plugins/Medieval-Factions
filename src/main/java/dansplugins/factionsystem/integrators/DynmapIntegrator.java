/*
  Copyright (c) 2022 Daniel McCoy Stephenson
  GPL3 License
 */
package dansplugins.factionsystem.integrators;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.helper.ChunkFlags;
import dansplugins.factionsystem.services.LocalLocaleService;
import dansplugins.factionsystem.utils.Logger;
import dansplugins.fiefs.utils.UUIDChecker;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.*;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Caibinus
 * @author Daniel McCoy Stephenson
 */
public class DynmapIntegrator {
    public static boolean dynmapInitialized = false;
    public boolean updateClaimsAreaMarkers = false;
    private static DynmapIntegrator instance = null;
    private final Map<String, AreaMarker> claimedChunkAreaMarkers = new HashMap<>();
    private final Map<String, Marker> claimedChunkMarkers = new HashMap<>();
    private final Map<String, AreaMarker> realmAreaMarkers = new HashMap<>();
    private final Map<String, Marker> realmMarkers = new HashMap<>();
    private final Plugin dynmapPlugin;
    private MarkerAPI markerAPI;
    private MarkerSet claims;
    private MarkerSet realms;

    enum direction {
        POSITIVE_X,
        POSITIVE_Z,
        NEGATIVE_X,
        NEGATIVE_Z
    }

    public DynmapIntegrator() {
        PluginManager pluginManager = getServer().getPluginManager();

        dynmapPlugin = pluginManager.getPlugin("dynmap");

        if(!isDynmapPresent()) {
            Logger.getInstance().log(LocalLocaleService.getInstance().getText("CannotFindDynmap"));
        }
        else {
            try {
                DynmapCommonAPI dynmapAPI = (DynmapCommonAPI) dynmapPlugin;
                markerAPI = dynmapAPI.getMarkerAPI();
                initializeMarkerSets();
                Logger.getInstance().log(LocalLocaleService.getInstance().getText("DynmapIntegrationSuccessful"));
            }
            catch (Exception e) {
                Logger.getInstance().log(LocalLocaleService.getInstance().getText("ErrorIntegratingWithDynmap") + e.getMessage());
            }
        }
    }

    public static boolean hasDynmap() {
        if (!dynmapInitialized) {
            return getInstance() != null;
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
                    DynmapIntegrator.getInstance().updateFactionInformation();
                    DynmapIntegrator.getInstance().updateRealmInformation();
                    DynmapIntegrator.getInstance().updateClaimsAreaMarkers = false;
                }
            }
        }.runTaskTimer(MedievalFactions.getInstance(), 40, interval);
    }

    /***
     * Tell the scheduled task that we have made changes and it should update the area markers.
     */
    public void updateClaims() {
        if (!isDynmapPresent()) {
            return;
        }

        if (DynmapIntegrator.hasDynmap()) {
            DynmapIntegrator.getInstance().updateClaimsAreaMarkers = true;
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
                Logger.getInstance().log(LocalLocaleService.getInstance().getText("ErrorCreatingMarkerSet") + ": markerLabel = " + markerLabel);
                return set;
            }
        }
        set.setMarkerSetLabel(markerLabel);
        return set;
    }

    private boolean isDynmapPresent() {
        return (dynmapPlugin != null);
    }

    private MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private void floodFillTarget(ChunkFlags source, ChunkFlags destination, int x, int y) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[] { x, y });

        while(!stack.isEmpty()) {
            int[] next = stack.pop();
            x = next[0];
            y = next[1];
            if(source.getFlag(x, y)) {
                source.setFlag(x, y, false);
                destination.setFlag(x, y, true);
                if(source.getFlag(x+1, y))
                    stack.push(new int[] { x+1, y });
                if(source.getFlag(x-1, y))
                    stack.push(new int[] { x-1, y });
                if(source.getFlag(x, y+1))
                    stack.push(new int[] { x, y+1 });
                if(source.getFlag(x, y-1))
                    stack.push(new int[] { x, y-1 });
            }
        }
    }
    
    private void updateRealmInformation() {
        Map<String,AreaMarker> newMap = new HashMap<>();
        Map<String,Marker> newmark = new HashMap<>();

        PersistentData.getInstance().loopThroughRealmsAndBuildAreaMarkersColoredInTheSameColor(realms, newMap);

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldAreaMarker : realmAreaMarkers.values()) {
            oldAreaMarker.deleteMarker();
        }
        for(Marker oldMarker : realmMarkers.values()) {
            oldMarker.deleteMarker();
        }
        /* And replace with new map */
        realmAreaMarkers.putAll(newMap);
        realmMarkers.putAll(newmark);
    }

    private void updateFactionInformation() {
        Map<String,AreaMarker> newMap = new HashMap<>();
        Map<String,Marker> newMarker = new HashMap<>();

        PersistentData.getInstance().loopThroughFactionsAndBuildColoredFactionAreaMarkers(realms, newMap);

        /* Now, review old map - anything left is gone */
        for(AreaMarker oldMap : claimedChunkAreaMarkers.values()) {
            oldMap.deleteMarker();
        }
        for(Marker oldMarker : claimedChunkMarkers.values()) {
            oldMarker.deleteMarker();
        }
        /* And replace with new map */
        claimedChunkAreaMarkers.putAll(newMap);
        claimedChunkMarkers.putAll(newMarker);
    }

    public String buildNationPopupText(Faction f) {
        UUIDChecker uuidChecker = new UUIDChecker();
        String message = "<h4>" + f.getName() + "</h4>" +
                "Owner: " + uuidChecker.findPlayerNameBasedOnUUID(f.getOwner()) + "<br/>" +
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
                                PersistentData.getInstance().getChunkDataAccessor().getChunksClaimedByFaction(f.getName()),
                                                                    f.getCumulativePowerLevel());
        return message;
    }

    public void updateDynmapForFaction(MarkerSet markerSet, Map<String, AreaMarker> areaMarkers, String type, String name, String popupDescription, String colorCode, Map<String, AreaMarker> newmap) {
        double[] x;
        double[] z;
        int indexOfPolygonForGivenFaction = 0;

        if (PersistentData.getInstance().getNumClaimedChunks() == 0)
            return;

        HashMap<String, ChunkFlags> chunkFlagMaps = new HashMap<>();
        LinkedList<ClaimedChunk> nodeValues = new LinkedList<>();
        String currentWorld = null;
        ChunkFlags currentChunkFlags = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
        PersistentData.getInstance().loopThroughChunksAndSetFlagsForWorlds(currentWorld, currentChunkFlags, chunkFlagMaps, nodeValues);

        // loop through until we don't find more areas
        while(nodeValues != null) {
            LinkedList<ClaimedChunk> nodes = null;
            LinkedList<ClaimedChunk> newNodes = null;
            ChunkFlags nodeChunkFlags = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for(ClaimedChunk node : nodeValues) {
                int nodex = node.getChunk().getX();
                int nodez = node.getChunk().getZ();

                // if not started, switch to world for this block first
                if(nodeChunkFlags == null) {
                    if(!node.getWorldName().equalsIgnoreCase(currentWorld)) {
                        currentWorld = node.getWorldName();
                        currentChunkFlags = chunkFlagMaps.get(currentWorld);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
                if((nodeChunkFlags == null) && currentChunkFlags.getFlag(nodex, nodez)) {
                    nodeChunkFlags = new ChunkFlags();  /* Create map for shape */
                    nodes = new LinkedList<ClaimedChunk>();
                    floodFillTarget(currentChunkFlags, nodeChunkFlags, nodex, nodez);   /* Copy shape */
                    nodes.add(node); /* Add it to our node list */
                    minx = nodex; minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if((nodeChunkFlags != null) && (node.getWorldName().equalsIgnoreCase(currentWorld)) &&
                        (nodeChunkFlags.getFlag(nodex, nodez))) {
                    nodes.add(node);
                    if(nodex < minx) {
                        minx = nodex; minz = nodez;
                    }
                    else if((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                }
                else {  /* Else, keep it in the list for the next polygon */
                    if(newNodes == null) newNodes = new LinkedList<ClaimedChunk>();
                    newNodes.add(node);
                }
            }
            nodeValues = newNodes; /* Replace list (null if no more to process) */
            if(nodeChunkFlags != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int init_x = minx;
                int init_z = minz;
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.POSITIVE_X;
                ArrayList<int[]> linelist = new ArrayList<int[]>();
                linelist.add(new int[] { init_x, init_z } ); // Add start point
                while((cur_x != init_x) || (cur_z != init_z) || (dir != direction.NEGATIVE_Z)) {
                    switch(dir) {
                        case POSITIVE_X: /* Segment in X+ direction */
                            if(!nodeChunkFlags.getFlag(cur_x+1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.POSITIVE_Z;  /* Change direction */
                            }
                            else if(!nodeChunkFlags.getFlag(cur_x+1, cur_z-1)) {  /* Straight? */
                                cur_x++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z }); /* Finish line */
                                dir = direction.NEGATIVE_Z;
                                cur_x++; cur_z--;
                            }
                            break;
                        case POSITIVE_Z: /* Segment in Z+ direction */
                            if(!nodeChunkFlags.getFlag(cur_x, cur_z+1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.NEGATIVE_X;  /* Change direction */
                            }
                            else if(!nodeChunkFlags.getFlag(cur_x+1, cur_z+1)) {  /* Straight? */
                                cur_z++;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x+1, cur_z+1 }); /* Finish line */
                                dir = direction.POSITIVE_X;
                                cur_x++; cur_z++;
                            }
                            break;
                        case NEGATIVE_X: /* Segment in X- direction */
                            if(!nodeChunkFlags.getFlag(cur_x-1, cur_z)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.NEGATIVE_Z;  /* Change direction */
                            }
                            else if(!nodeChunkFlags.getFlag(cur_x-1, cur_z+1)) {  /* Straight? */
                                cur_x--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z+1 }); /* Finish line */
                                dir = direction.POSITIVE_Z;
                                cur_x--; cur_z++;
                            }
                            break;
                        case NEGATIVE_Z: /* Segment in Z- direction */
                            if(!nodeChunkFlags.getFlag(cur_x, cur_z-1)) { /* Right turn? */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.POSITIVE_X;  /* Change direction */
                            }
                            else if(!nodeChunkFlags.getFlag(cur_x-1, cur_z-1)) {  /* Straight? */
                                cur_z--;
                            }
                            else {  /* Left turn */
                                linelist.add(new int[] { cur_x, cur_z }); /* Finish line */
                                dir = direction.NEGATIVE_X;
                                cur_x--; cur_z--;
                            }
                            break;
                    }
                }

                // build information for specific area
                String polyid = name + "__" + type + "__" + indexOfPolygonForGivenFaction;
                int csize = 16;
                int sz = linelist.size();
                x = new double[sz];
                z = new double[sz];
                for(int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double)line[0] * (double)csize;
                    z[i] = (double)line[1] * (double)csize;
                }

                // find an existing area
                AreaMarker areaMarker = areaMarkers.remove(polyid); // is area existent
                if (areaMarker == null) {
                    areaMarker = markerSet.createAreaMarker(polyid, name, false, currentWorld, x, z, false);
                    if (areaMarker == null) {
                        System.out.println(String.format(LocalLocaleService.getInstance().getText("ErrorAddingAreaMarker"), polyid));
                        return;
                    }
                }
                else {
                    areaMarker.setCornerLocations(x, z); // replace corner locations
                    areaMarker.setLabel(name);   // update label
                }
                try {
                    int colrCode = Integer.decode(colorCode);
                    if (type.equalsIgnoreCase("realm")) {
                        areaMarker.setLineStyle(4, 1.0, colrCode);
                        areaMarker.setFillStyle(0.0, colrCode);
                    }
                    else {
                        areaMarker.setLineStyle(1, 1.0, colrCode);
                        areaMarker.setFillStyle(0.3, colrCode);
                    }
                }
                catch (Exception e) {
                    System.out.printf((LocalLocaleService.getInstance().getText("ErrorSettingAreaMarkerColor")) + "%n", colorCode);
                }
                areaMarker.setDescription(popupDescription); // set popup

                // add to map
                newmap.put(polyid, areaMarker);
                indexOfPolygonForGivenFaction++;
            }
        }
    }

    /**
     * Dynmap marker set id (prefix used for other ids/layer ids)
     */
    private String getDynmapPluginSetId(String type) { return "mf.faction." + type; }

    /**
     * @return Dynmap set Id for faction.
     */
    private String getDynmapFactionSetId(String holder) {
        return getDynmapPluginSetId("holder") + "." + holder;
    }

    /**
     * @return Dynmap layer Id for faction.
     */
    private String getDynmapPluginLayer() {
        return getDynmapPluginSetId("layer") + "_layer";
    }

    /**
     * @return Dynmap polygon Id corresponding to these chunk coordinates.
     */
    private String getDynmapChunkPolyId(int x, int z) {
        return getDynmapPluginSetId("poly") + "_" + String.format("%d-%d", x, z);
    }

    /**
     *
     * Refreshes the Dynmap Player List for the nation that owns the current chunk.
     */
    private void refreshDynmapPlayerListForChunkHolder(String holder) {
        try {
            String setID = getDynmapFactionSetId(holder);
            MarkerAPI markerAPI = getMarkerAPI();
            Set<String> playerNames = PersistentData.getInstance().getPlayerNamesViaPowerRecordsOfMembersOfAFaction(holder);

            // does set exist?
            PlayerSet set = markerAPI.getPlayerSet(setID);
            if (set == null) {
                markerAPI.createPlayerSet(setID, true, playerNames, false);
            }
            else {
                set.setPlayers(playerNames);
            }
        } catch (Exception ignored) {

        }
    }
}