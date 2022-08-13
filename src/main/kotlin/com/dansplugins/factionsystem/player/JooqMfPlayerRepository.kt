package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_PLAYER
import com.dansplugins.factionsystem.jooq.tables.records.MfPlayerRecord
import org.jooq.DSLContext
import org.jooq.impl.DSL.*

class JooqMfPlayerRepository(private val dsl: DSLContext) : MfPlayerRepository {
    override fun getPlayer(id: MfPlayerId) =
        dsl.selectFrom(MF_PLAYER)
            .where(MF_PLAYER.ID.eq(id.value))
            .fetchOne()
            ?.toDomain()

    override fun upsert(player: MfPlayer): MfPlayer {
        val rowCount = dsl.insertInto(MF_PLAYER)
            .set(MF_PLAYER.ID, player.id.value)
            .set(MF_PLAYER.VERSION, 1)
            .set(MF_PLAYER.POWER, player.power)
            .onConflict(MF_PLAYER.ID).doUpdate()
            .set(MF_PLAYER.POWER, player.power)
            .set(MF_PLAYER.VERSION, player.version + 1)
            .execute()
        if (rowCount == 0) throw OptimisticLockingFailureException("Invalid version: ${player.version}")
        return getPlayer(player.id).let(::requireNotNull)
    }

    override fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>) {
        val maxPower = 20
        val hoursToReachMax = 12
        val timeIncrementHours = 1
        dsl.update(MF_PLAYER)
            .set(MF_PLAYER.POWER, least(value(maxPower), greatest(value(0),
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
            .execute()
    }

    override fun decreaseOfflinePlayerPower(onlinePlayerIds: List<MfPlayerId>) {
        val maxPower = 20
        val hoursToReachMin = 72
        val timeIncrementHours = 1
        dsl.update(MF_PLAYER)
            .set(MF_PLAYER.POWER, least(value(maxPower), greatest(value(0),
                MF_PLAYER.POWER.div(maxPower)
                    .pow(0.25)
                    .times(hoursToReachMin)
                    .plus(timeIncrementHours)
                    .div(hoursToReachMin)
                    .pow(4)
                    .times(maxPower)
            )))
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(MF_PLAYER.ID.notIn(onlinePlayerIds.map { it.value }))
            .execute()
    }

    private fun MfPlayerRecord.toDomain() = MfPlayer(MfPlayerId(id), power)
}