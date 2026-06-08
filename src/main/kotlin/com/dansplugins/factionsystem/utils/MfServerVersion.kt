package com.dansplugins.factionsystem.utils

import org.bukkit.Bukkit

object MfServerVersion {

    private fun parse(): Triple<Int, Int, Int> {
        val versionPart = Bukkit.getBukkitVersion().substringBefore('-')
        val parts = versionPart.split('.')
        return Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }

    fun isAtLeast(major: Int, minor: Int = 0, patch: Int = 0): Boolean {
        val (actualMajor, actualMinor, actualPatch) = parse()
        if (actualMajor > major) return true
        if (actualMajor == major && actualMinor > minor) return true
        if (actualMajor == major && actualMinor == minor && actualPatch >= patch) return true
        return false
    }
}
