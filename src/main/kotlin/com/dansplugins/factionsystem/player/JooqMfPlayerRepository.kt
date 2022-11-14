package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_PLAYER
import com.dansplugins.factionsystem.jooq.tables.records.MfPlayerRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL.*

class JooqMfPlayerRepository(private val plugin: MedievalFactions, private val dsl: DSLContext) : MfPlayerRepository {
    override fun getPlayer(id: MfPlayerId) =
        dsl.selectFrom(MF_PLAYER)
            .where(MF_PLAYER.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()

    override fun getPlayers(): List<MfPlayer> =
        dsl.selectFrom(MF_PLAYER)
            .fetch()
            .map { it.toDomain() }

    override fun upsert(player: MfPlayer): MfPlayer {
        val rowCount = dsl.insertInto(MF_PLAYER)
            .set(MF_PLAYER.ID, player.id.value)
            .set(MF_PLAYER.VERSION, 1)
            .set(MF_PLAYER.NAME, player.name)
            .set(MF_PLAYER.POWER, player.power)
            .set(MF_PLAYER.POWER_AT_LOGOUT, player.powerAtLogout)
            .set(MF_PLAYER.BYPASS_ENABLED, player.isBypassEnabled)
            .set(MF_PLAYER.CHAT_CHANNEL, player.chatChannel?.name)
            .onConflict(MF_PLAYER.ID).doUpdate()
            .set(MF_PLAYER.NAME, player.name)
            .set(MF_PLAYER.POWER, player.power)
            .set(MF_PLAYER.POWER_AT_LOGOUT, player.powerAtLogout)
            .set(MF_PLAYER.BYPASS_ENABLED, player.isBypassEnabled)
            .set(MF_PLAYER.CHAT_CHANNEL, player.chatChannel?.name)
            .set(MF_PLAYER.VERSION, player.version + 1)
            .where(MF_PLAYER.ID.eq(player.id.value))
            .and(MF_PLAYER.VERSION.eq(MF_PLAYER.VERSION))
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${player.version}")
        return getPlayer(player.id).let(::requireNotNull)
    }

    override fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>) {
        val maxPower = plugin.config.getDouble("players.maxPower")
        val hoursToReachMax = plugin.config.getDouble("players.hoursToReachMaxPower")
        val timeIncrementHours = 0.25
        dsl.update(MF_PLAYER)
            .set(MF_PLAYER.POWER, least(value(maxPower), greatest(value(0.0),
                value((hoursToReachMax * 2) + timeIncrementHours)
                    .minus(
                        MF_PLAYER.POWER.div(maxPower)
                            .minus(1)
                            .div(-1)
                            .pow(0.25)
                            .plus(1)
                            .times(hoursToReachMax)
                    )
                    .div(hoursToReachMax)
                    .minus(1)
                    .pow(4)
                    .times(-1)
                    .plus(1)
                    .times(maxPower)
            )))
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(MF_PLAYER.ID.`in`(onlinePlayerIds.map { it.value }))
            .and(MF_PLAYER.POWER.lt(maxPower))
            .execute()
    }

    override fun decreaseOfflinePlayerPower(onlinePlayerIds: List<MfPlayerId>) {
        val maxPower = plugin.config.getDouble("players.maxPower")
        val hoursToReachMin = plugin.config.getDouble("players.hoursToReachMinPower")
        val timeIncrementHours = 0.25
        dsl.update(MF_PLAYER)
            .set(MF_PLAYER.POWER, least(value(maxPower), greatest(value(0.0),
                least(MF_PLAYER.POWER, MF_PLAYER.POWER_AT_LOGOUT, maxPower).div(MF_PLAYER.POWER_AT_LOGOUT)
                    .minus(1)
                    .div(-1)
                    .pow(0.25)
                    .times(hoursToReachMin)
                    .plus(timeIncrementHours)
                    .div(hoursToReachMin)
                    .pow(4)
                    .times(-1)
                    .plus(1)
                    .times(MF_PLAYER.POWER_AT_LOGOUT)
            )))
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(MF_PLAYER.ID.notIn(onlinePlayerIds.map { it.value }))
            .and(MF_PLAYER.POWER_AT_LOGOUT.gt(0.0))
            .and(MF_PLAYER.POWER.gt(0.0))
            .execute()
    }

    private fun MfPlayerRecord.toDomain() = MfPlayer(
        MfPlayerId(id),
        version,
        name,
        power,
        powerAtLogout,
        bypassEnabled,
        chatChannel?.let(MfFactionChatChannel::valueOf)
    )
}