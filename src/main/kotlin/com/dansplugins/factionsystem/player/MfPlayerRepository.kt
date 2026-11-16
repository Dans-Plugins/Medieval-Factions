package com.dansplugins.factionsystem.player

interface MfPlayerRepository {

    fun getPlayer(id: MfPlayerId): MfPlayer?
    fun getPlayers(): List<MfPlayer>
    fun upsert(player: MfPlayer): MfPlayer

    /**
     * Moves each online player's power up the gain curve towards their maximum power: `players.leaderMaxPower` for
     * the players in [leaderIds] when that setting is set ([MfMaxPower]), `players.maxPower` for everyone else.
     */
    fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>, leaderIds: Set<MfPlayerId> = emptySet())

    /** Moves each offline player's power down the decay curve, capped at their maximum power as for the gain. */
    fun decreaseOfflinePlayerPower(onlinePlayerIds: List<MfPlayerId>, leaderIds: Set<MfPlayerId> = emptySet())

    /**
     * Lowers the power of every player above their maximum power to that maximum (see [MfMaxPower]): a player who
     * stopped being a leader, or a leader after `players.leaderMaxPower` was lowered. Power at or below the maximum
     * is never changed.
     */
    fun clampPowerToMax(leaderIds: Set<MfPlayerId>)
}
