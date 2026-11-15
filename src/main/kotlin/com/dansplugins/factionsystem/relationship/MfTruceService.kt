package com.dansplugins.factionsystem.relationship

import com.dansplugins.factionsystem.faction.MfFactionId
import org.bukkit.configuration.file.YamlConfiguration
import java.io.File
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Tracks truces between factions that have made peace (`factions.truceDurationMinutes`).
 *
 * A truce starts when two factions are fully at peace again and stops either faction declaring war on the
 * other until it ends. A duration of `0` or less turns truces off: nothing is recorded and nothing is
 * enforced, which is the shipped default. Truce end times are kept in [file] so a restart does not end them.
 */
class MfTruceService(
    private val durationMinutes: () -> Long,
    private val file: File?,
    private val clock: () -> Instant = Instant::now
) {

    private val truceEnds = ConcurrentHashMap<String, Long>()

    init {
        load()
    }

    val isEnabled: Boolean
        get() = durationMinutes() > 0

    @JvmName("startTruce")
    fun startTruce(factionId: MfFactionId, targetId: MfFactionId) {
        val minutes = durationMinutes()
        if (minutes <= 0) return
        truceEnds[key(factionId, targetId)] = clock().plus(Duration.ofMinutes(minutes)).toEpochMilli()
        save()
    }

    /**
     * Returns the time left on the truce between the two factions, or `null` when there is none (or truces
     * are turned off). The order of the two factions does not matter.
     */
    @JvmName("getRemainingTruce")
    fun getRemainingTruce(factionId: MfFactionId, targetId: MfFactionId): Duration? {
        if (!isEnabled) return null
        val key = key(factionId, targetId)
        val end = truceEnds[key] ?: return null
        val remaining = end - clock().toEpochMilli()
        if (remaining <= 0) {
            truceEnds.remove(key)
            save()
            return null
        }
        return Duration.ofMillis(remaining)
    }

    /**
     * Returns the whole minutes left on the truce between the two factions, rounded up (at least 1), or `null`
     * when there is no truce. Used for the refusal messages.
     */
    @JvmName("getRemainingTruceMinutes")
    fun getRemainingTruceMinutes(factionId: MfFactionId, targetId: MfFactionId): Long? {
        val remaining = getRemainingTruce(factionId, targetId) ?: return null
        return maxOf(1L, (remaining.seconds + 59) / 60)
    }

    private fun key(factionId: MfFactionId, targetId: MfFactionId): String =
        listOf(factionId.value, targetId.value).sorted().joinToString("_")

    @Synchronized
    private fun load() {
        if (file == null || !file.exists()) return
        val yaml = YamlConfiguration.loadConfiguration(file)
        val now = clock().toEpochMilli()
        yaml.getKeys(false).forEach { key ->
            val end = yaml.getLong(key)
            if (end > now) truceEnds[key] = end
        }
    }

    @Synchronized
    private fun save() {
        if (file == null) return
        val yaml = YamlConfiguration()
        truceEnds.forEach { (key, end) -> yaml.set(key, end) }
        file.parentFile?.mkdirs()
        yaml.save(file)
    }
}
