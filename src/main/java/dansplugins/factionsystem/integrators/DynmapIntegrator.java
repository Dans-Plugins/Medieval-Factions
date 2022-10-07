package dansplugins.factionsystem.integrators;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.objects.domain.ClaimedChunk;
import dansplugins.factionsystem.objects.domain.Faction;
import dansplugins.factionsystem.objects.domain.PowerRecord;
import dansplugins.factionsystem.objects.helper.ChunkFlags;
import dansplugins.factionsystem.services.LocaleService;
import dansplugins.factionsystem.utils.Logger;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.PluginManager;
import org.bukkit.scheduler.BukkitRunnable;
import org.dynmap.DynmapCommonAPI;
import org.dynmap.markers.*;
import preponderous.ponder.minecraft.bukkit.tools.UUIDChecker;

import java.util.*;

import static org.bukkit.Bukkit.getServer;

/**
 * @author Caibinus
 */
public class DynmapIntegrator {
    public static boolean dynmapInitialized = false;
    private final Logger logger;
    private final LocaleService localeService;
    private final MedievalFactions medievalFactions;
    private final PersistentData persistentData;

    // Claims/factions markers
    private final Map<String, AreaMarker> resAreas = new HashMap<>();
    private final Map<String, Marker> resMark = new HashMap<>();

    // Dynmap integration related members
    // Realms markers
    private final Map<String, AreaMarker> realmsAreas = new HashMap<>();
    private final Map<String, Marker> realmsMark = new HashMap<>();
    private final Plugin dynmap;
    private boolean updateClaimsAreaMarkers = false;
    private MarkerSet claims;
    private MarkerSet realms;
    private MarkerAPI markerAPI;

    public DynmapIntegrator(Logger logger, LocaleService localeService, MedievalFactions medievalFactions, PersistentData persistentData) {
        this.logger = logger;
        this.localeService = localeService;
        this.medievalFactions = medievalFactions;
        this.persistentData = persistentData;
        PluginManager pm = getServer().getPluginManager();

        /* Get dynmap */
        dynmap = pm.getPlugin("dynmap");

        if (isDynmapMissing()) {
            this.logger.debug(this.localeService.get("CannotFindDynmap"));
        } else {
            try {
                DynmapCommonAPI dynmapAPI = (DynmapCommonAPI) dynmap; /* Get API */
                if (dynmapAPI == null) {
                    this.logger.error("Instantiated DynmapCommonAPI object was null. DynmapIntegrator construction cannot continue.");
                    return;
                }
                markerAPI = dynmapAPI.getMarkerAPI();
                initializeMarkerSets();
                this.logger.debug(this.localeService.get("DynmapIntegrationSuccessful"));
            } catch (Exception e) {
                this.logger.debug(this.localeService.get("ErrorIntegratingWithDynmap") + e.getMessage());
            }
        }
    }

    public static boolean hasDynmap() {
        return dynmapInitialized;
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
                if (isDynmapMissing()) {
                    return;
                }
                if (updateClaimsAreaMarkers) {
                    if (realms != null) {
                        realms.deleteMarkerSet();
                        claims.deleteMarkerSet();
                    }
                    initializeMarkerSets();
                    dynmapUpdateFactions();
                    dynmapUpdateRealms();
                    updateClaimsAreaMarkers = false;
                }
            }
        }.runTaskTimer(medievalFactions, 40, interval);
    }

    /***
     * Tell the scheduled task that we have made changes and it should update the
     * area markers.
     */
    public void updateClaims() {
        if (isDynmapMissing()) {
            return;
        }

        if (hasDynmap()) {
            updateClaimsAreaMarkers = true;
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
                logger.debug(localeService.get("ErrorCreatingMarkerSet") + ": markerLabel = " + markerLabel);
                return null;
            }
        }
        set.setMarkerSetLabel(markerLabel);
        return set;
    }

    private boolean isDynmapMissing() {
        logger.debug("Is dynmap missing? " + (dynmap == null));
        return (dynmap == null);
    }

    private MarkerAPI getMarkerAPI() {
        return markerAPI;
    }

    /**
     * Find all contiguous blocks, set in target and clear in source
     */
    private void floodFillTarget(ChunkFlags src, ChunkFlags dest, int x, int y) {
        ArrayDeque<int[]> stack = new ArrayDeque<>();
        stack.push(new int[]{x, y});

        while (!stack.isEmpty()) {
            int[] nxt = stack.pop();
            x = nxt[0];
            y = nxt[1];
            if (src.getFlag(x, y)) { /* Set in src */
                src.setFlag(x, y, false);   /* Clear source */
                dest.setFlag(x, y, true);   /* Set in destination */
                if (src.getFlag(x + 1, y))
                    stack.push(new int[]{x + 1, y});
                if (src.getFlag(x - 1, y))
                    stack.push(new int[]{x - 1, y});
                if (src.getFlag(x, y + 1))
                    stack.push(new int[]{x, y + 1});
                if (src.getFlag(x, y - 1))
                    stack.push(new int[]{x, y - 1});
            }
        }
    }

    /* Update Realm information */
    private void dynmapUpdateRealms() {
        // Realms Layer

        Map<String, AreaMarker> newMap = new HashMap<>(); /* Build new map */
        Map<String, Marker> newMark = new HashMap<>(); /* Build new map */

        /* Loop through realms and build area markers coloured in the same colour
            as each faction's liege's colour. */
        for (Faction f : persistentData.getFactions()) {
            String liegeName = f.getTopLiege();
            Faction liege = persistentData.getFaction(liegeName);
            String liegeColor;
            String popupText;
            // If there's no liege, then f is the liege.
            if (liege != null) {
                liegeColor = liege.getFlags().getFlag("dynmapTerritoryColor").toString();
                popupText = buildNationPopupText(liege);
            } else {
                liegeColor = f.getFlags().getFlag("dynmapTerritoryColor").toString();
                liegeName = f.getName() + "__parent";
                popupText = buildNationPopupText(f);
            }
            dynmapUpdateFaction(f, realms, newMap, "realm", liegeName + "__" + getClass().getName(), popupText, liegeColor, newMap);
        }

        /* Now, review old map - anything left is gone */
        for (AreaMarker oldm : realmsAreas.values()) {
            oldm.deleteMarker();
        }
        for (Marker oldm : realmsMark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        realmsAreas.putAll(newMap);
        realmsMark.putAll(newMark);
    }

    /* Update Faction information */
    private void dynmapUpdateFactions() {
        // Claims Layer

        Map<String, AreaMarker> newmap = new HashMap<>(); /* Build new map */
        Map<String, Marker> newmark = new HashMap<>(); /* Build new map */

        /* Loop through factions and build coloured faction area markers. */
        for (Faction f : persistentData.getFactions()) {
            dynmapUpdateFaction(f, claims, newmap, "claims", f.getName(), buildNationPopupText(f), f.getFlags().getFlag("dynmapTerritoryColor").toString(), newmap);
        }

        /* Now, review old map - anything left is gone */
        for (AreaMarker oldm : resAreas.values()) {
            oldm.deleteMarker();
        }
        for (Marker oldm : resMark.values()) {
            oldm.deleteMarker();
        }
        /* And replace with new map */
        resAreas.putAll(newmap);
        resMark.putAll(newmark);
    }

    private String buildNationPopupText(Faction f) {
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
                persistentData.getChunkDataAccessor().getChunksClaimedByFaction(f.getName()),
                f.getCumulativePowerLevel());
        return message;
    }

    private void dynmapUpdateFaction(Faction faction, MarkerSet markerSet, Map<String, AreaMarker> areaMarkers, String type, String name, String popupDescription, String colorCode, Map<String, AreaMarker> newmap) {
        double[] x;
        double[] z;
        int poly_index = 0; /* Index of polygon for given town */

        /* Handle areas */
        List<ClaimedChunk> blocks = faction.getClaimedChunks();
        if (blocks.isEmpty())
            return;
        HashMap<String, ChunkFlags> blkmaps = new HashMap<>();
        LinkedList<ClaimedChunk> nodevals = new LinkedList<>();
        String currentWorld = null;
        ChunkFlags curblks = null;

        /* Loop through blocks: set flags on blockmaps for worlds */
        for (ClaimedChunk b : blocks) {
            if (!b.getWorldName().equalsIgnoreCase(currentWorld)) { /* Not same world */
                String wname = b.getWorldName();
                currentWorld = b.getWorldName();
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
        while (nodevals != null) {
            LinkedList<ClaimedChunk> ournodes = null;
            LinkedList<ClaimedChunk> newlist = null;
            ChunkFlags ourblks = null;
            int minx = Integer.MAX_VALUE;
            int minz = Integer.MAX_VALUE;
            for (ClaimedChunk node : nodevals) {
                int nodex = node.getChunk().getX();
                int nodez = node.getChunk().getZ();
                if (ourblks == null) {   /* If not started, switch to world for this block first */
                    if (!node.getWorldName().equalsIgnoreCase(currentWorld)) {
                        currentWorld = node.getWorldName();
                        curblks = blkmaps.get(currentWorld);
                    }
                }
                /* If we need to start shape, and this block is not part of one yet */
                if ((ourblks == null) && curblks.getFlag(nodex, nodez)) {
                    ourblks = new ChunkFlags();  /* Create map for shape */
                    ournodes = new LinkedList<>();
                    floodFillTarget(curblks, ourblks, nodex, nodez);   /* Copy shape */
                    ournodes.add(node); /* Add it to our node list */
                    minx = nodex;
                    minz = nodez;
                }
                /* If shape found, and we're in it, add to our node list */
                else if ((ourblks != null) && (node.getWorldName().equalsIgnoreCase(currentWorld)) &&
                        (ourblks.getFlag(nodex, nodez))) {
                    ournodes.add(node);
                    if (nodex < minx) {
                        minx = nodex;
                        minz = nodez;
                    }
                    else if ((nodex == minx) && (nodez < minz)) {
                        minz = nodez;
                    }
                } else {  /* Else, keep it in the list for the next polygon */
                    if (newlist == null) {
                        newlist = new LinkedList<>();
                    }
                    newlist.add(node);
                }
            }
            nodevals = newlist; /* Replace list (null if no more to process) */
            if (ourblks != null) {
                /* Trace outline of blocks - start from minx, minz going to x+ */
                int cur_x = minx;
                int cur_z = minz;
                direction dir = direction.XPLUS;
                ArrayList<int[]> linelist = new ArrayList<>();
                linelist.add(new int[]{minx, minz}); // Add start point
                while ((cur_x != minx) || (cur_z != minz) || (dir != direction.ZMINUS)) {
                    switch (dir) {
                        case XPLUS: /* Segment in X+ direction */
                            if (!ourblks.getFlag(cur_x + 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z - 1)) {  /* Straight? */
                                cur_x++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z}); /* Finish line */
                                dir = direction.ZMINUS;
                                cur_x++;
                                cur_z--;
                            }
                            break;
                        case ZPLUS: /* Segment in Z+ direction */
                            if (!ourblks.getFlag(cur_x, cur_z + 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x + 1, cur_z + 1)) {  /* Straight? */
                                cur_z++;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x + 1, cur_z + 1}); /* Finish line */
                                dir = direction.XPLUS;
                                cur_x++;
                                cur_z++;
                            }
                            break;
                        case XMINUS: /* Segment in X- direction */
                            if (!ourblks.getFlag(cur_x - 1, cur_z)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZMINUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z + 1)) {  /* Straight? */
                                cur_x--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z + 1}); /* Finish line */
                                dir = direction.ZPLUS;
                                cur_x--;
                                cur_z++;
                            }
                            break;
                        case ZMINUS: /* Segment in Z- direction */
                            if (!ourblks.getFlag(cur_x, cur_z - 1)) { /* Right turn? */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XPLUS;  /* Change direction */
                            } else if (!ourblks.getFlag(cur_x - 1, cur_z - 1)) {  /* Straight? */
                                cur_z--;
                            } else {  /* Left turn */
                                linelist.add(new int[]{cur_x, cur_z}); /* Finish line */
                                dir = direction.XMINUS;
                                cur_x--;
                                cur_z--;
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
                for (int i = 0; i < sz; i++) {
                    int[] line = linelist.get(i);
                    x[i] = (double) line[0] * (double) csize;
                    z[i] = (double) line[1] * (double) csize;
                }
                /* Find existing one */
                AreaMarker m = areaMarkers.remove(polyid); /* Existing area? */
                if (m == null) {
                    m = markerSet.createAreaMarker(polyid, name, false, currentWorld, x, z, false);
                    if (m == null) {
                        System.out.printf((localeService.get("ErrorAddingAreaMarker")) + "%n", polyid);
                        return;
                    }
                } else {
                    m.setCornerLocations(x, z); /* Replace corner locations */
                    m.setLabel(name);   /* Update label */
                }
                try {
                    int colrCode = Integer.decode(colorCode);
                    if (type.equalsIgnoreCase("realm")) {
                        m.setLineStyle(4, 1.0, colrCode);
                        m.setFillStyle(0.0, colrCode);
                    } else {
                        m.setLineStyle(1, 1.0, colrCode);
                        m.setFillStyle(0.3, colrCode);
                    }
                } catch (Exception e) {
                    System.out.printf((localeService.get("ErrorSettingAreaMarkerColor")) + "%n", colorCode);
                }
                m.setDescription(popupDescription); /* Set popup */

                /* Add to map */
                newmap.put(polyid, m);
                poly_index++;
            }
        }
    }

    /***
     * Dynmap marker set id (prefix used for other ids/layer ids)
     */
    private String getDynmapPluginSetId(String type) {
        return "mf.faction." + type;
    }

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

    @SuppressWarnings("unused")
    private String getDynmapChunkPolyId(String worldName, int x, int z) {
        // return getDynmapFactionSetId() + "_" + String.format("%d-%d", chunk.getX(), chunk.getZ());
        return getDynmapPluginSetId("poly") + "_" + String.format("%d-%d", x, z);
    }

    /***
     *
     * Refreshes the Dynmap Player List for the nation that owns the current chunk.
     */

    @SuppressWarnings("unused")
    private void dynmapUpdateNationPlayerLists(String holder) {
        try {
            String setid = getDynmapFactionSetId(holder);
            MarkerAPI markerapi = getMarkerAPI();
            Set<String> plids = new HashSet<>();
            Faction f = persistentData.getFaction(holder);
            if (f != null) {
                for (PowerRecord powerRecord : persistentData.getPlayerPowerRecords()) {
                    Faction pf = persistentData.getPlayersFaction(powerRecord.getPlayerUUID());
                    if (pf != null && pf.getName().equalsIgnoreCase(holder)) {
                        UUIDChecker uuidChecker = new UUIDChecker();
                        plids.add(uuidChecker.findPlayerNameBasedOnUUID(powerRecord.getPlayerUUID()));
                    }
                }
            }
            PlayerSet set = markerapi.getPlayerSet(setid);  /* See if set exists */
            if (set == null) {
                markerapi.createPlayerSet(setid, true, plids, false);
            } else {
                set.setPlayers(plids);
            }
        } catch (Exception e) {
            logger.error("Something went wrong updating a nation's player lists.");
        }
    }

    enum direction {XPLUS, ZPLUS, XMINUS, ZMINUS}
}