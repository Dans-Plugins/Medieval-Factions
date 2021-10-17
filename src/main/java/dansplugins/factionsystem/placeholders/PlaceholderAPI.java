package dansplugins.factionsystem.placeholders;

import dansplugins.factionsystem.MedievalFactions;
import dansplugins.factionsystem.data.PersistentData;
import dansplugins.factionsystem.managers.ChunkManager;
import dansplugins.factionsystem.objects.ClaimedChunk;
import dansplugins.factionsystem.objects.Faction;
import dansplugins.factionsystem.objects.PowerRecord;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.Bukkit;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

public class PlaceholderAPI extends PlaceholderExpansion {

    @Override
    public @NotNull String getIdentifier() {
        return MedievalFactions.getInstance().getName();
    }

    @Override
    public @NotNull String getAuthor() {
        return String.join(", ", MedievalFactions.getInstance().getDescription().getAuthors());
    }

    @Override
    public @NotNull String getVersion() {
        return MedievalFactions.getInstance().getVersion();
    }

    @Override
    public boolean persist() {
        return true;
    }

    @Override
    public boolean canRegister() {
        return true;
    }

    @Override
    public String onPlaceholderRequest(Player player, @NotNull String id) {
        id = id.toLowerCase(); // I'm unsure if PlaceholderAPI enforces case, but lets just do it to make sure.
        if (player == null) return null; // We only want to handle Player-Placeholders here.

        final boolean hasFaction = PersistentData.getInstance().isInFaction(player.getUniqueId());
        final Faction faction = PersistentData.getInstance().getPlayersFaction(player.getUniqueId());

        // Prerequisites.
        if (id.startsWith("faction_") && !hasFaction && !id.equalsIgnoreCase("faction_at_location")) {
            return "Factionless"; // We don't want Faction-Specific Placeholders to return if they are Factionless!
        }

        // Faction-Specific.
        if (id.equalsIgnoreCase("faction_name")) {
            // The name of the Faction the Player is in.
            return faction.getName();
        }
        if (id.equalsIgnoreCase("faction_prefix")) {
            // The prefix of the Faction the Player is in.
            return faction.getPrefix();
        }
        if (id.equalsIgnoreCase("faction_total_claimed_chunks")) {
            // The total chunks claimed for the Faction that the Player is in.
            return String.valueOf(faction.getClaimedChunks().size());
        }
        if (id.equalsIgnoreCase("faction_cumulative_power")) {
            // The cumulative power (power+bonus_power) for the Faction that the Player is in.
            return String.valueOf(faction.getCumulativePowerLevel());
        }
        if (id.equalsIgnoreCase("faction_bonus_power")) {
            // The bonus power for the Faction that the Player is in.
            return String.valueOf(faction.getBonusPower());
        }
        if (id.equalsIgnoreCase("faction_power")) {
            // The power (cumulative-bonus_power) for the Faction that the Player is in.
            return String.valueOf(faction.getCumulativePowerLevel()-faction.getBonusPower());
        }
        if (id.equalsIgnoreCase("faction_ally_count")) {
            // The total amount of Allies the Faction has that the Player is in.
            return String.valueOf(faction.getAllies().size());
        }
        if (id.equalsIgnoreCase("faction_enemy_count")) {
            // The total amount of Enemies the Faction has that the Player is in.
            return String.valueOf(faction.getEnemyFactions().size());
        }
        if (id.equalsIgnoreCase("faction_gate_count")) {
            // The total amount of Gates the Faction has that the Player is in.
            return String.valueOf(faction.getTotalGates());
        }
        if (id.equalsIgnoreCase("faction_vassal_count")) {
            // The total number of Vassals that the Faction has that the Player is in.
            return String.valueOf(faction.getNumVassals());
        }
        if (id.equalsIgnoreCase("faction_liege")) {
            // The Liege for the Faction or N/A that the Player is in.
            return faction.hasLiege() ? faction.getLiege() : "N/A";
        }
        if (id.equalsIgnoreCase("faction_leader")) {
            // The Leader of the Faction that the Player is in.
            return Bukkit.getOfflinePlayer(faction.getOwner()).getName();
        }
        if (id.equalsIgnoreCase("faction_population")) {
            // The total players/members/population for the Faction that the Player is in.
            return String.valueOf(faction.getPopulation());
        }
        if (id.equalsIgnoreCase("faction_officers")) {
            // The total officers for the Faction that the Player is in.
            return String.valueOf(faction.getNumOfficers());
        }
        if (id.equalsIgnoreCase("faction_rank")) {
            // The Player-Specific rank for their Faction. (Owner/Officer/Member).
            if (faction.isOwner(player.getUniqueId())) {
                return "Owner";
            } else if (faction.isOfficer(player.getUniqueId())) {
                return "Officer";
            } else {
                return "Member";
            }
        }
        if (id.equalsIgnoreCase("faction_player_power")) {
            // The player-specific power which counts toward their Faction's power.
            return String.valueOf(PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).getPowerLevel());
        }
        if (id.equalsIgnoreCase("faction_player_max_power")) {
            // The player-specific max_power which is their total contribute-able power toward their Faction's power.
            return String.valueOf(PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId()).maxPower());
        }
        if (id.equalsIgnoreCase("faction_player_power_full")) {
            // The formatted version of the 'power' and 'max_power' placeholders, 10/10 for example.
            final PowerRecord playersPowerRecord = PersistentData.getInstance().getPlayersPowerRecord(player.getUniqueId());
            return playersPowerRecord.getPowerLevel() + "/" + playersPowerRecord.maxPower();
        }

        // Player-Specific.
        if (id.equalsIgnoreCase("player_chunk_location")) {
            // The Player's location (chunk coordinates), useful for Scoreboards.
            final Chunk chunk = player.getLocation().getChunk();
            return chunk.getX() + ":" + chunk.getZ();
        }
        if (id.equalsIgnoreCase("player_location")) {
            // The Player's specific location, X:Y:Z which is also useful for Scoreboards.
            final Location location = player.getLocation();
            return location.getBlockX() + ":" + location.getBlockY() + ":" + location.getBlockZ();
        }
        if (id.equalsIgnoreCase("player_world")) {
            // The name of the World for the current Player-Location.
            try {
                return Objects.requireNonNull(player.getLocation().getWorld()).getName();
            } catch (Exception ex) {
                return "World Undefined"; // This won't ever throw, but IntelliJ wouldn't let me compile without it :/
            }
        }
        if (id.equalsIgnoreCase("player_total_logins")) {
            // The total amount of times a Player has logged in.
            return String.valueOf(PersistentData.getInstance().getPlayerActivityRecord(player.getUniqueId()).getLogins());
        }
        if (id.equalsIgnoreCase("player_session_length")) {
            // The total time since their current login. (Days:Hours:Minutes:Seconds) or (Hours:Minutes:Seconds).
            return PersistentData.getInstance().getPlayerActivityRecord(player.getUniqueId()).getActiveSessionLength();
        }
        if (id.equalsIgnoreCase("faction_at_location")) {
            // The Faction at the Player's current location. (Wilderness if nothing).
            ClaimedChunk claim = ChunkManager.getInstance().getClaimedChunk(player.getLocation().getChunk());
            if (claim == null) return "Wilderness";
            else return claim.getHolder();
        }

        return null; // This is required by PlaceholderAPI if there is no matching Placeholder.
    }

}
