package com.dansplugins.factionsystem.player

interface MfPlayerRepository {

    fun getPlayer(id: MfPlayerId): MfPlayer?
    fun getPlayers(): List<MfPlayer>
    fun upsert(player: MfPlayer): MfPlayer
    fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>)
    fun decreaseOfflinePlayerPower(onlinePlayerIds: List<MfPlayerId>)
}
