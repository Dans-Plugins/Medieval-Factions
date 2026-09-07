package com.dansplugins.factionsystem.utils

import org.bukkit.Bukkit

object MfServerVersion {

    var versionProvider: () -> String = { Bukkit.getBukkitVersion() }

    private var version: Triple<Int, Int, Int>? = null

    private fun getVersion(): Triple<Int, Int, Int> {
        val cached = version
        if (cached != null) return cached
        val versionPart = versionProvider().substringBefore('-')
        val parts = versionPart.split('.')
        return Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        ).also { version = it }
    }

    internal fun resetForTesting() {
        version = null
    }

    fun isAtLeast(major: Int, minor: Int = 0, patch: Int = 0): Boolean {
        val (actualMajor, actualMinor, actualPatch) = getVersion()
        if (actualMajor > major) return true
        if (actualMajor == major && actualMinor > minor) return true
        if (actualMajor == major && actualMinor == minor && actualPatch >= patch) return true
        return false
    }
}
