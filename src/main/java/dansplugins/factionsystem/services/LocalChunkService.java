package dansplugins.factionsystem.services;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.utils.Messenger;
import dansplugins.factionsystem.data.EphemeralData;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.events.FactionClaimEvent;
import dansplugins.factionsystem.events.FactionUnclaimEvent;
import dansplugins.factionsystem.objects.domain.*;
import dansplugins.factionsystem.utils.BlockChecker;
import dansplugins.factionsystem.utils.InteractionAccessChecker;
import org.bukkit.*;
import org.bukkit.block.Block;
import org.bukkit.entity.Player;
import org.bukkit.event.player.PlayerInteractEvent;

import java.time.ZonedDateTime;
import java.util.*;

import static org.bukkit.Bukkit.getServer;
import static org.bukkit.Material.LADDER;

/**
 * This class provides a local service to Medieval Factions in the management of claimed chunks.
 * @author Daniel Stephenson
 */
public class LocalChunkService {

    private static LocalChunkService instance;

    /**
     * This constructor ensures that only this class can instantiate itself.
     */
    private LocalChunkService() {

    }

    /**
     * This is the method that can be utilized to access the singleton instance of the Local Chunk Service.
     * @return The singleton instance of the Local Chunk Service.
     */
    public static LocalChunkService getInstance() {
        if (instance == null) {
            instance = new LocalChunkService();
        }
        return instance;
    }

    /**
     * This public method can be used to retrieve a claimed chunk. A returned value of null means the chunk is not claimed.
     * @param chunk The chunk to grab.
     * @return The associated claimed chunk.
     */
    public ClaimedChunk getClaimedChunk(Chunk chunk) {
        return getClaimedChunk(chunk.getX(), chunk.getZ(), chunk.getWorld().getName());
    }

    /**
     * This method can be used to unclaim a radius of chunks around a player.
     * @param radius The radius of chunks to unclaim.
     * @param player The player unclaiming the chunks.
     * @param faction The player's faction.
     */
    public void radiusUnclaimAtLocation(int radius, Player player, Faction faction) {
        final int maxChunksUnclaimable = 999;
        if (radius<=0||radius> maxChunksUnclaimable) {
            final LocalLocaleService instance = LocalLocaleService.getInstance();
            player.sendMessage(ChatColor.RED + String.format(instance.getText("RadiusRequirement"), maxChunksUnclaimable));
            return;
        }
        final Set<Chunk> chunkSet = obtainChunks(player.getLocation().getChunk(), radius);
        chunkSet.stream()
                .map(c -> isChunkClaimed(c.getX(), c.getZ(), c.getWorld().getName()))
                .filter(Objects::nonNull)
                .forEach(chunk -> removeChunk(chunk, player, faction));
    }

    /**
     * This method can be used to claim a radius of chunks around a player.
     * @param depth The radius of chunks to claim.
     * @param claimant The player claiming the chunks.
     * @param location The central location of claiming.
     * @param claimantsFaction The claimant's faction.
     */
    public void radiusClaimAtLocation(int depth, Player claimant, Location location, Faction claimantsFaction) {
        int maxClaimRadius = MedievalFactions.getInstance().getConfig().getInt("maxClaimRadius");
        if (depth < 0 || depth > maxClaimRadius) {
            claimant.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("RadiusRequirement"), maxClaimRadius));
            return;
        }
        if (depth == 0) {
            claimChunkAtLocation(claimant, location, claimantsFaction);
            return;
        }
        final Chunk initial = location.getChunk();
        final Set<Chunk> chunkSet = obtainChunks(initial, depth);
        chunkSet.forEach(chunk -> claimChunkAtLocation(
                claimant, getChunkCoords(chunk), chunk.getWorld(), claimantsFaction
        ));
    }

    /**
     * Claims a singular chunk at a location.
     * @param claimant The player claiming the chunk.
     * @param location The location getting claimed.
     * @param claimantsFaction The player's faction.
     */
    public void claimChunkAtLocation(Player claimant, Location location, Faction claimantsFaction) {
        double[] chunkCoords = getChunkCoords(location);
        claimChunkAtLocation(claimant, chunkCoords, location.getWorld(), claimantsFaction);
    }

    /**
     * Unclaims a chunk at a location.
     * @param player The player unclaiming the chunk.
     * @param playersFaction The player's faction.
     */
    public void removeChunkAtPlayerLocation(Player player, Faction playersFaction) { // TODO: fix unclaim error here?
        // get player coordinates
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();

        // handle admin bypass
        if (EphemeralData.getInstance().getAdminsBypassingProtections().contains(player.getUniqueId())) {
            ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
            if (chunk != null) {
                removeChunk(chunk, player, PersistentData.getInstance().getFaction(chunk.getHolder()));
                player.sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("LandClaimedUsingAdminBypass"));
                return;
            }
            player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("LandNotCurrentlyClaimed"));
            return;
        }

        ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());

        // ensure that chunk is claimed
        if (chunk == null) {
            return;
        }

        // ensure that the chunk is claimed by the player's faction.
        if (!chunk.getHolder().equalsIgnoreCase(playersFaction.getName())) {
            player.sendMessage(ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("LandClaimedBy"), chunk.getHolder()));
            return;
        }

        // initiate removal
        removeChunk(chunk, player, playersFaction);
        player.sendMessage(ChatColor.GREEN + LocalLocaleService.getInstance().getText("LandUnclaimed"));
    }

    /**
     * This can be used to check which faction has laid claim to a chunk.
     * @param player The player whose location we will be checking.
     * @return The name of the faction that has claimed the chunk. A value of "unclaimed" will be returned if the chunk is unclaimed.
     */
    public String checkOwnershipAtPlayerLocation(Player player) {
        double[] playerCoords = new double[2];
        playerCoords[0] = player.getLocation().getChunk().getX();
        playerCoords[1] = player.getLocation().getChunk().getZ();
        ClaimedChunk chunk = isChunkClaimed(playerCoords[0], playerCoords[1], player.getLocation().getWorld().getName());
        if (chunk != null)
        {
            return chunk.getHolder();
        }
        return "unclaimed"; // TODO: return null instead
    }

    /**
     * Checks if a gate is in a chunk.
     * @param gate The gate to check.
     * @param chunk The claimed chunk to check.
     * @return Whether the gate is in the claimed chunk.
     */
    public boolean isGateInChunk(Gate gate, ClaimedChunk chunk)
    {
        if ((gate.getTopLeftChunkX() == chunk.getCoordinates()[0] || gate.getBottomRightChunkX() == chunk.getCoordinates()[0])
                && (gate.getTopLeftChunkZ() == chunk.getCoordinates()[1] || gate.getBottomRightChunkZ() == chunk.getCoordinates()[1]))
        {
            return true;
        }
        return false;
    }

    /**
     * This can be used to retrieve the number of chunks claimed by a faction.
     * @param factionName The name of the faction we are checking.
     * @param claimedChunks A reference to the claimed chunks in the PersistentData class.
     * @return An integer indicating how many chunks have been claimed by this faction.
     */
    public int getChunksClaimedByFaction(String factionName, ArrayList<ClaimedChunk> claimedChunks) { // TODO: replace passed claimedChunks with singleton reference
        int counter = 0;
        for (ClaimedChunk chunk : claimedChunks) {
            if (chunk.getHolder().equalsIgnoreCase(factionName)) {
                counter++;
            }
        }
        return counter;
    }

    /**
     *  This can be used to check if a chunk is claimed.
     * @param chunk The chunk we are checking.
     * @param claimedChunks A reference to the claimed chunks in the PersistentData class.
     * @return A boolean indicating if the chunk is claimed.
     */
    public boolean isClaimed(Chunk chunk, ArrayList<ClaimedChunk> claimedChunks) { // TODO: replace passed claimedChunks with singleton reference
        for (ClaimedChunk claimedChunk : claimedChunks) {
            if (claimedChunk.getCoordinates()[0] == chunk.getX() && claimedChunk.getCoordinates()[1] == chunk.getZ() && claimedChunk.getWorld().equalsIgnoreCase(chunk.getWorld().getName())) {
                return true;
            }
        }
        return false;
    }

    /**
     * This can be used to unclaim every chunk that a faction owns.
     * @param factionName The name of the faction we are removing all claimed chunks from.
     * @param claimedChunks A reference to the claimed chunks in the PersistentData class.
     */
    public void removeAllClaimedChunks(String factionName, ArrayList<ClaimedChunk> claimedChunks) { // TODO: replace passed claimedChunks with singleton reference

        Iterator<ClaimedChunk> itr = claimedChunks.iterator();

        while (itr.hasNext()) {
            ClaimedChunk currentChunk = itr.next();
            if (currentChunk.getHolder().equalsIgnoreCase(factionName)) {
                try {
                    itr.remove();
                }
                catch(Exception e) {
                    System.out.println(LocalLocaleService.getInstance().getText("ErrorClaimedChunkRemoval"));
                }
            }
        }
    }

    /**
     * This can be used to check if a faction has more claimed land than power.
     * @param faction The faction we are checking.
     * @param claimedChunks A reference to the claimed chunks in the PersistentData class.
     * @return Whether the faction's claimed land exceeds their power.
     */
    public boolean isFactionExceedingTheirDemesneLimit(Faction faction, ArrayList<ClaimedChunk> claimedChunks) { // TODO: replace passed claimedChunks with singleton reference
        return (getChunksClaimedByFaction(faction.getName(), claimedChunks) > faction.getCumulativePowerLevel());
    }

    /**
     * If a player is exceeding their demesne limit, this method will inform them.
     * @param player The player to inform.
     * @param factions A reference to the factions in the PersistentData class.
     * @param claimedChunks A reference to the claimed chunks in the PersistentData class.
     */
    public void informPlayerIfTheirLandIsInDanger(Player player, ArrayList<Faction> factions, ArrayList<ClaimedChunk> claimedChunks) { // TODO: replace passed claimedChunks with singleton reference and remove factions parameter
        Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());
        if (faction != null) {
            if (isFactionExceedingTheirDemesneLimit(faction, claimedChunks)) {
                player.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertMoreClaimedChunksThanPower"));
            }
        }
    }

    /**
     * This handles interaction within a claimed chunk for the PlayerInteractEvent event.
     * @param event The PlayerInteractEvent event.
     * @param claimedChunk The chunk that has been interacted with.
     */
    public void handleClaimedChunkInteraction(PlayerInteractEvent event, ClaimedChunk claimedChunk) {
        // player not in a faction and isn't overriding
        if (!PersistentData.getInstance().isInFaction(event.getPlayer().getUniqueId()) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

            Block block = event.getClickedBlock();
            if (MedievalFactions.getInstance().getConfig().getBoolean("nonMembersCanInteractWithDoors") && block != null && BlockChecker.getInstance().isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            event.setCancelled(true);
        }

        // TODO: simplify this code with a call to the shouldEventBeCancelled() method in InteractionAccessChecker.java

        final Faction playersFaction = PersistentData.getInstance().getPlayersFaction(event.getPlayer().getUniqueId());
        if (playersFaction == null) {
            return;
        }

        // if player's faction is not the same as the holder of the chunk and player isn't overriding
        if (!(playersFaction.getName().equalsIgnoreCase(claimedChunk.getHolder())) && !EphemeralData.getInstance().getAdminsBypassingProtections().contains(event.getPlayer().getUniqueId())) {

            Block block = event.getClickedBlock();
            if (MedievalFactions.getInstance().getConfig().getBoolean("nonMembersCanInteractWithDoors") && block != null && BlockChecker.getInstance().isDoor(block)) {
                // allow non-faction members to interact with doors
                return;
            }

            // if enemy territory
            if (playersFaction.isEnemy(claimedChunk.getHolder())) {
                // if not interacting with chest
                if (isBlockInteractable(event)) {
                    // allow placing ladders
                    if (MedievalFactions.getInstance().getConfig().getBoolean("laddersPlaceableInEnemyFactionTerritory")) {
                        if (event.getMaterial() == LADDER) {
                            return;
                        }
                    }
                    // allow eating
                    if (materialAllowed(event.getMaterial())) {
                        return;
                    }
                    // allow blocking
                    if (event.getPlayer().getInventory().getItemInOffHand().getType() == Material.SHIELD) {
                        return;
                    }
                }
            }

            if (!InteractionAccessChecker.getInstance().isOutsiderInteractionAllowed(event.getPlayer(), claimedChunk, playersFaction)) {
                event.setCancelled(true);
                return;
            }
        }
    }

    /**
     * This can be used to forcefully claim a chunk at the players location, regardless of requirements.
     * @param player The player whose location we are using.
     * @param faction The faction we are claiming the chunk for.
     */
    public void forceClaimAtPlayerLocation(Player player, Faction faction) {
        Location location = player.getLocation();

        ClaimedChunk claimedChunk = getClaimedChunk(location.getChunk());

        if (claimedChunk != null) {
            removeChunk(claimedChunk, player, faction);
        }

        addClaimedChunk(location.getChunk(), faction, location.getWorld());
    }

    /**
     * This is a private method intended to be used by this class to retrieve a claimed chunk.
     * @param x The x coordinate of the chunk to retrieve.
     * @param z The z coordinate of the chunk to retrieve.
     * @param world The world that the chunk to retrieve is in.
     * @return The claimed chunk at the given location. A value of null indicates that the chunk is not claimed.
     */
    private ClaimedChunk getClaimedChunk(int x, int z, String world) {
        for (ClaimedChunk claimedChunk : PersistentData.getInstance().getClaimedChunks()) {
            if (claimedChunk.getCoordinates()[0] == x && claimedChunk.getCoordinates()[1] == z && claimedChunk.getWorld().equalsIgnoreCase(world)) {
                return claimedChunk;
            }
        }
        return null;
    }

    private Set<Chunk> obtainChunks(Chunk initial, int radius) {
        final Set<Chunk> chunkSet = new HashSet<>(); // Avoid duplicates without checking for it yourself.
        for (int x = initial.getX() - radius; x <= initial.getX() + radius; x++) {
            for (int z = initial.getZ() - radius; z <= initial.getZ() + radius; z++) {
                chunkSet.add(initial.getWorld().getChunkAt(x, z));
            }
        }
        return chunkSet;
    }

    private void claimChunkAtLocation(Player claimant, double[] chunkCoords, World world, Faction claimantsFaction) {

        // if demesne limit enabled
        if (LocalConfigService.getInstance().getBoolean("limitLand")) {
            // if at demesne limit
            if (!(getChunksClaimedByFaction(claimantsFaction.getName(), PersistentData.getInstance().getClaimedChunks()) < claimantsFaction.getCumulativePowerLevel())) {
                claimant.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("AlertReachedDemesne"));
                return;
            }
        }

        // check if land is already claimed
        ClaimedChunk chunk = isChunkClaimed(chunkCoords[0], chunkCoords[1], world.getName());
        if (chunk != null) {
            // chunk already claimed
            Faction targetFaction = PersistentData.getInstance().getFaction(chunk.getHolder());

            // if holder is player's faction
            if (targetFaction.getName().equalsIgnoreCase(claimantsFaction.getName()) && !claimantsFaction.getAutoClaimStatus()) {
                claimant.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("LandAlreadyClaimedByYourFaction"));
                return;
            }

            // if not at war with target faction or inactive claiming isn't possible
            if (!claimantsFaction.isEnemy(targetFaction.getName()) || !everyPlayerInFactionExperiencingPowerDecay(targetFaction)) {
                claimant.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("MustBeAtWarOrFactionMustBeInactive"));
                return;
            }

            // surrounded chunk protection check
            if (MedievalFactions.getInstance().getConfig().getBoolean("surroundedChunksProtected")) {
                if (isClaimedChunkSurroundedByChunksClaimedBySameFaction(chunk)) {
                    claimant.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("SurroundedChunkProtected"));
                    return;
                }
            }

            int targetFactionsCumulativePowerLevel = targetFaction.getCumulativePowerLevel();
            int chunksClaimedByTargetFaction = getChunksClaimedByFaction(targetFaction.getName(), PersistentData.getInstance().getClaimedChunks());

            // if target faction does not have more land than their demesne limit
            if (!(targetFactionsCumulativePowerLevel < chunksClaimedByTargetFaction)) {
                claimant.sendMessage(ChatColor.RED + LocalLocaleService.getInstance().getText("TargetFactionNotOverClaiming"));
                return;
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

            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, chunk.getChunk());
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (!claimEvent.isCancelled()) {
                PersistentData.getInstance().getClaimedChunks().remove(chunk);

                Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
                addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                claimant.sendMessage(ChatColor.GREEN + String.format(LocalLocaleService.getInstance().getText("AlertLandConqueredFromAnotherFaction"), targetFaction.getName(), getChunksClaimedByFaction(claimantsFaction.getName(), PersistentData.getInstance().getClaimedChunks()), claimantsFaction.getCumulativePowerLevel()));

                Messenger.getInstance().sendAllPlayersInFactionMessage(targetFaction, ChatColor.RED + String.format(LocalLocaleService.getInstance().getText("AlertLandConqueredFromYourFaction"), claimantsFaction.getName()));
            }
        }
        else {

            Chunk toClaim = world.getChunkAt((int) chunkCoords[0], (int) chunkCoords[1]);
            FactionClaimEvent claimEvent = new FactionClaimEvent(claimantsFaction, claimant, toClaim);
            Bukkit.getPluginManager().callEvent(claimEvent);
            if (!claimEvent.isCancelled()) {
                // chunk not already claimed
                addClaimedChunk(toClaim, claimantsFaction, claimant.getWorld());
                claimant.sendMessage(ChatColor.GREEN + String.format(LocalLocaleService.getInstance().getText("AlertLandClaimed"), getChunksClaimedByFaction(claimantsFaction.getName(), PersistentData.getInstance().getClaimedChunks()), claimantsFaction.getCumulativePowerLevel()));
            }
        }
    }

    private void addClaimedChunk(Chunk chunk, Faction faction, World world) {
        ClaimedChunk newChunk = new ClaimedChunk(chunk);
        newChunk.setHolder(faction.getName());
        newChunk.setWorld(world.getName());
        PersistentData.getInstance().getClaimedChunks().add(newChunk);
    }

    private double[] getChunkCoords(Location location) {
        double[] chunkCoords = new double[2];
        chunkCoords[0] = location.getChunk().getX();
        chunkCoords[1] = location.getChunk().getZ();
        return chunkCoords;
    }

    private double[] getChunkCoords(Chunk chunk) {
        double[] chunkCoords = new double[2];
        chunkCoords[0] = chunk.getX();
        chunkCoords[1] = chunk.getZ();
        return chunkCoords;
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
            ActivityRecord record = PersistentData.getInstance().getPlayerActivityRecord(uuid);
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
                ActivityRecord newRecord = new ActivityRecord(uuid, 1);
                newRecord.setLastLogout(ZonedDateTime.now());
                PersistentData.getInstance().getPlayerActivityRecords().add(newRecord);
            }
        }
        return (numExperiencingPowerDecay == faction.getMemberArrayList().size());
    }

    private void removeChunk(ClaimedChunk chunk, Player player, Faction faction) { // TODO: make sure the chunk being removed is actually claimed by the player's faction
        // String identifier = (int)chunk.getChunk().getX() + "_" + (int)chunk.getChunk().getZ();

        FactionUnclaimEvent unclaimEvent = new FactionUnclaimEvent(faction, player, chunk.getChunk());
        Bukkit.getPluginManager().callEvent(unclaimEvent);
        if (unclaimEvent.isCancelled()) {
            // TODO: add locale message
            return;
        }

        // if faction home is located on this chunk
        Location factionHome = faction.getFactionHome();
        if (factionHome != null) {
            if (factionHome.getChunk().getX() == chunk.getChunk().getX() && factionHome.getChunk().getZ() == chunk.getChunk().getZ()
                    && chunk.getWorld().equalsIgnoreCase(player.getLocation().getWorld().getName())) {
                // remove faction home
                faction.setFactionHome(null);
                Messenger.getInstance().sendAllPlayersInFactionMessage(faction, ChatColor.RED + LocalLocaleService.getInstance().getText("AlertFactionHomeRemoved"));

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

    private boolean isBlockInteractable(PlayerInteractEvent event) {
        if (event.getClickedBlock() != null) {
            // CHEST
            if (BlockChecker.getInstance().isChest(event.getClickedBlock())) {
                return false;
            }
            switch(event.getClickedBlock().getType()) {
                case ACACIA_DOOR:
                case BIRCH_DOOR:
                case DARK_OAK_DOOR:
                case IRON_DOOR:
                case JUNGLE_DOOR:
                case OAK_DOOR:
                case SPRUCE_DOOR:
                case ACACIA_TRAPDOOR:
                case BIRCH_TRAPDOOR:
                case DARK_OAK_TRAPDOOR:
                case IRON_TRAPDOOR:
                case JUNGLE_TRAPDOOR:
                case OAK_TRAPDOOR:
                case SPRUCE_TRAPDOOR:
                case ACACIA_FENCE_GATE:
                case BIRCH_FENCE_GATE:
                case DARK_OAK_FENCE_GATE:
                case JUNGLE_FENCE_GATE:
                case OAK_FENCE_GATE:
                case SPRUCE_FENCE_GATE:
                case BARREL:
                case LEVER:
                case ACACIA_BUTTON:
                case BIRCH_BUTTON:
                case DARK_OAK_BUTTON:
                case JUNGLE_BUTTON:
                case OAK_BUTTON:
                case SPRUCE_BUTTON:
                case STONE_BUTTON:
                case LECTERN:
                    return false;
            }
        }
        return true;
    }

    private boolean materialAllowed(Material material) {
        switch(material) {
            case BREAD:
            case POTATO:
            case CARROT:
            case BEETROOT:
            case BEEF:
            case PORKCHOP:
            case CHICKEN:
            case COD:
            case SALMON:
            case MUTTON:
            case RABBIT:
            case TROPICAL_FISH:
            case PUFFERFISH:
            case MUSHROOM_STEW:
            case RABBIT_STEW:
            case BEETROOT_SOUP:
            case COOKED_BEEF:
            case COOKED_PORKCHOP:
            case COOKED_CHICKEN:
            case COOKED_SALMON:
            case COOKED_MUTTON:
            case COOKED_COD:
            case MELON:
            case PUMPKIN:
            case MELON_SLICE:
            case CAKE:
            case PUMPKIN_PIE:
            case APPLE:
            case COOKIE:
            case POISONOUS_POTATO:
            case CHORUS_FRUIT:
            case DRIED_KELP:
            case BAKED_POTATO:
                return true;
        }
        return false;
    }
}
