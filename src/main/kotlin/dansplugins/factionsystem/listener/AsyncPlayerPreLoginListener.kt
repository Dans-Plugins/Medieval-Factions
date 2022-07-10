package dansplugins.factionsystem.listener

import dansplugins.factionsystem.player.MfPlayer
import dansplugins.factionsystem.player.MfPlayerId
import dansplugins.factionsystem.player.MfPlayerService
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.AsyncPlayerPreLoginEvent

class AsyncPlayerPreLoginListener(private val playerService: MfPlayerService) : Listener {

    @EventHandler
    fun onAsyncPlayerPreLogin(event: AsyncPlayerPreLoginEvent) {
        playerService.save(MfPlayer(MfPlayerId(event.uniqueId.toString())))
    }

}