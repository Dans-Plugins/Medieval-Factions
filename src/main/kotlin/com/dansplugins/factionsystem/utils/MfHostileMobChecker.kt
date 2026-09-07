package com.dansplugins.factionsystem.utils

import org.bukkit.entity.Entity
import org.bukkit.entity.Monster

object MfHostileMobChecker {

    fun isHostileMob(entity: Entity): Boolean {
        if (MfServerVersion.isAtLeast(1, 19, 3)) {
            try {
                val enemyClass = Class.forName("org.bukkit.entity.Enemy")
                return enemyClass.isInstance(entity)
            } catch (_: ClassNotFoundException) {
                return entity is Monster
            }
        }
        return entity is Monster
    }
}
