package com.dansplugins.factionsystem.update

import com.dansplugins.factionsystem.MedievalFactions
import org.bukkit.ChatColor.YELLOW
import org.bukkit.command.CommandSender
import org.bukkit.event.EventHandler
import org.bukkit.event.Listener
import org.bukkit.event.player.PlayerJoinEvent
import java.util.logging.Level

/**
 * Runs the update check once, off the main thread, and tells operators about a
 * newer release: one console line when the check finishes, and a chat line to
 * every player with [PERMISSION] who is online then or joins later. See
 * [MfUpdateCheck] for the decisions and `update-check` in config.yml for the
 * opt-out.
 */
class MfUpdateNotifier(
    private val plugin: MedievalFactions,
    private val fetchLatestRelease: () -> MfLatestRelease? = MfGitHubReleaseFetcher(
        "${plugin.name}/${plugin.description.version} (update check)",
        plugin.logger
    )
) : Listener {

    @Volatile
    var notice: MfUpdateNotice? = null
        private set

    /** Starts the check if it is not opted out. Returns immediately. */
    fun start() {
        // One-argument getter: falls through to the jar's bundled default (true)
        // when the key is missing from the server's config.yml. The two-argument
        // form would return its explicit fallback instead.
        val reason = MfUpdateCheck.disabledReason(
            configEnabled = plugin.config.getBoolean(MfUpdateCheck.CONFIG_KEY),
            environmentOptsOut = MfUpdateCheck.environmentOptsOut()
        )
        if (reason != null) {
            plugin.logger.fine("Update check is off ($reason).")
            return
        }
        plugin.server.pluginManager.registerEvents(this, plugin)
        plugin.server.scheduler.runTaskAsynchronously(plugin, Runnable { check() })
    }

    private fun check() {
        val found = try {
            MfUpdateCheck.noticeFor(plugin.description.version, fetchLatestRelease())
        } catch (exception: Exception) {
            plugin.logger.log(Level.FINE, "Update check failed: ${exception.message}", exception)
            null
        } ?: return
        notice = found
        plugin.logger.info(found.consoleMessage())
        if (!plugin.isEnabled) return
        plugin.server.scheduler.runTask(
            plugin,
            Runnable { plugin.server.onlinePlayers.forEach { tell(it, found) } }
        )
    }

    @EventHandler
    fun onPlayerJoin(event: PlayerJoinEvent) {
        val found = notice ?: return
        tell(event.player, found)
    }

    private fun tell(sender: CommandSender, found: MfUpdateNotice) {
        if (!sender.hasPermission(PERMISSION)) return
        val reason = when (found.reason) {
            MfUpdateNotice.Reason.H2_SHUTDOWN_CORRUPTION -> plugin.language["UpdateNoticeReasonH2Shutdown"]
            MfUpdateNotice.Reason.GENERIC -> plugin.language["UpdateNoticeReasonGeneric"]
        }
        sender.sendMessage(
            "$YELLOW" + plugin.language["UpdateNoticeNewerRelease", found.latestVersion, found.currentVersion, reason, found.url]
        )
    }

    companion object {
        const val PERMISSION = "mf.updatenotice"
    }
}
