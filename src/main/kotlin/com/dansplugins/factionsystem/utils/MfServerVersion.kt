package com.dansplugins.factionsystem.utils

import org.bukkit.Bukkit

object MfServerVersion {

    var versionProvider: () -> String = { Bukkit.getBukkitVersion() }
    private val version: Triple<Int, Int, Int> by lazy { parse() }

    private fun parse(): Triple<Int, Int, Int> {
        val versionPart = versionProvider().substringBefore('-')
        val parts = versionPart.split('.')
        return Triple(
            parts.getOrNull(0)?.toIntOrNull() ?: 0,
            parts.getOrNull(1)?.toIntOrNull() ?: 0,
            parts.getOrNull(2)?.toIntOrNull() ?: 0
        )
    }


    fun isAtLeast(major: Int, minor: Int = 0, patch: Int = 0): Boolean {
        val (actualMajor, actualMinor, actualPatch) = version
        if (actualMajor > major) return true
        if (actualMajor == major && actualMinor > minor) return true
        if (actualMajor == major && actualMinor == minor && actualPatch >= patch) return true
        return false
    }
}
