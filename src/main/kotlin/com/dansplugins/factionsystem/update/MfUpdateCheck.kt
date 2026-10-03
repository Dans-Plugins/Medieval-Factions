package com.dansplugins.factionsystem.update

import com.dansplugins.factionsystem.trace.TraceClient

/** The newest published, non-pre-release release, as the releases API reports it. */
data class MfLatestRelease(
    val tagName: String,
    val url: String
)

/** What an operator is told: the newer version, where to read about it, and why it matters. */
data class MfUpdateNotice(
    val currentVersion: String,
    val latestVersion: String,
    val url: String,
    val reason: Reason
) {
    enum class Reason {
        /** The running version is older than 6.0.0, which closes the H2 database cleanly on shutdown. */
        H2_SHUTDOWN_CORRUPTION,

        /** Nothing specific is known; the operator is pointed at the release notes. */
        GENERIC
    }

    /** The one console line, in English like the plugin's other startup lines. */
    fun consoleMessage(): String {
        val why = when (reason) {
            Reason.H2_SHUTDOWN_CORRUPTION -> "it fixes database corruption on shutdown (H2)"
            Reason.GENERIC -> "see the release notes for what changed"
        }
        return "A newer release of Medieval Factions is available: $latestVersion (this server runs $currentVersion) - $why: $url" +
            " (turn this check off with update-check.enabled: false in config.yml)"
    }
}

/**
 * Decides whether the startup update check runs, and whether a fetched release
 * is worth telling an operator about. Holds no Bukkit or network state, so it
 * is tested with plain values; [MfUpdateNotifier] wires it to the server.
 */
object MfUpdateCheck {

    const val CONFIG_KEY = "update-check.enabled"

    /** Reason reported when `update-check.enabled` is false. */
    const val REASON_CONFIG = "config.yml"

    /** Reason reported when `TRACE_USAGE_REPORTING` or `DO_NOT_TRACK` opts out. */
    const val REASON_ENVIRONMENT = TraceClient.REASON_ENVIRONMENT

    /** Releases before this one do not close the H2 database on shutdown, which corrupts it over many restarts. */
    val H2_SHUTDOWN_FIX: MfReleaseVersion = MfReleaseVersion(6, 0, 0)

    /**
     * Why the check does not run, or null when it runs. The environment wins
     * over the config, matching usage reporting.
     */
    fun disabledReason(configEnabled: Boolean, environmentOptsOut: Boolean): String? = when {
        environmentOptsOut -> REASON_ENVIRONMENT
        !configEnabled -> REASON_CONFIG
        else -> null
    }

    /**
     * Whether `TRACE_USAGE_REPORTING` (off/false/0/no) or `DO_NOT_TRACK`
     * (1/true/yes) is set. Asked of the vendored trace client rather than
     * re-implemented, so the two switches always mean the same thing for usage
     * reporting and for this check: the client consults the environment before
     * anything else, so a client built switched-off reports "environment" as its
     * reason exactly when the environment opts out. Building it starts no thread.
     */
    fun environmentOptsOut(): Boolean =
        TraceClient.disabled().disabledReason() == TraceClient.REASON_ENVIRONMENT

    /**
     * The notice for [latest], or null when there is nothing to say: either
     * version is unrecognised, or the release is not newer than [currentVersion].
     * A pre-release build such as 7.0.1-SNAPSHOT is newer than 7.0.0, so it is
     * never told to "upgrade" to it.
     */
    fun noticeFor(currentVersion: String, latest: MfLatestRelease?): MfUpdateNotice? {
        if (latest == null) return null
        val current = MfReleaseVersion.parse(currentVersion) ?: return null
        val available = MfReleaseVersion.parse(latest.tagName) ?: return null
        if (available.isPreRelease) return null
        if (available <= current) return null
        val reason = if (current < H2_SHUTDOWN_FIX && available >= H2_SHUTDOWN_FIX) {
            MfUpdateNotice.Reason.H2_SHUTDOWN_CORRUPTION
        } else {
            MfUpdateNotice.Reason.GENERIC
        }
        return MfUpdateNotice(currentVersion, available.toString(), latest.url, reason)
    }
}
