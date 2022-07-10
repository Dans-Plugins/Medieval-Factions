package dansplugins.factionsystem.player

import org.bukkit.Bukkit
import org.bukkit.OfflinePlayer
import java.util.*

data class MfPlayer(
    val id: MfPlayerId
) {
    fun toBukkit() = Bukkit.getOfflinePlayer(UUID.fromString(id.value))

    companion object {
        fun fromBukkit(player: OfflinePlayer) = MfPlayer(MfPlayerId(player.uniqueId.toString()))
    }
}