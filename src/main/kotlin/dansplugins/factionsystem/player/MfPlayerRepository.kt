package dansplugins.factionsystem.player

interface MfPlayerRepository {

    fun getPlayer(id: MfPlayerId): MfPlayer?
    fun upsert(player: MfPlayer): MfPlayer

}