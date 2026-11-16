package com.dansplugins.factionsystem.slimefun

/**
 * The actions Slimefun asks protection plugins about. The names match Slimefun's own `Interaction`
 * constants, so this enum can stay free of Slimefun classes and be used where Slimefun is absent.
 */
enum class MfSlimefunAction {
    BREAK_BLOCK,
    PLACE_BLOCK,
    INTERACT_BLOCK,
    INTERACT_ENTITY,
    ATTACK_ENTITY,
    ATTACK_PLAYER
}
