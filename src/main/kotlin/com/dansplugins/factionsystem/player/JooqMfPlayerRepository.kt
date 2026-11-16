package com.dansplugins.factionsystem.player

import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.chat.MfFactionChatChannel
import com.dansplugins.factionsystem.failure.OptimisticLockingFailureException
import com.dansplugins.factionsystem.jooq.Tables.MF_PLAYER
import com.dansplugins.factionsystem.jooq.tables.records.MfPlayerRecord
import org.jooq.Condition
import org.jooq.DSLContext
import org.jooq.impl.DSL.greatest
import org.jooq.impl.DSL.least
import org.jooq.impl.DSL.value

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
        // The conflict is signalled with null rather than thrown inside the transaction: jOOQ
        // wraps a checked exception thrown there in a DataAccessException, which would hide the
        // OptimisticLockingFailureException from MfPlayerService's CONFLICT mapping.
        return upsertIfCurrent(player) ?: throw OptimisticLockingFailureException("Invalid version: ${player.version}")
    }

    private fun upsertIfCurrent(player: MfPlayer): MfPlayer? = dsl.transactionResult { config ->
        val transactionalDsl = config.dsl()
        val rowCount = transactionalDsl.insertInto(MF_PLAYER)
            .set(MF_PLAYER.ID, player.id.value)
            .set(MF_PLAYER.VERSION, 1)
            .set(MF_PLAYER.NAME, player.name)
            .set(MF_PLAYER.POWER, player.power)
            .set(MF_PLAYER.POWER_AT_LOGOUT, player.powerAtLogout)
            .set(MF_PLAYER.BYPASS_ENABLED, player.isBypassEnabled)
            .set(MF_PLAYER.CHAT_CHANNEL, player.chatChannel?.name)
            .set(MF_PLAYER.FIXED_POWER, player.fixedPower)
            .onConflict(MF_PLAYER.ID).doUpdate()
            .set(MF_PLAYER.NAME, player.name)
            .set(MF_PLAYER.POWER, player.power)
            .set(MF_PLAYER.POWER_AT_LOGOUT, player.powerAtLogout)
            .set(MF_PLAYER.BYPASS_ENABLED, player.isBypassEnabled)
            .set(MF_PLAYER.CHAT_CHANNEL, player.chatChannel?.name)
            .set(MF_PLAYER.FIXED_POWER, player.fixedPower)
            .set(MF_PLAYER.VERSION, player.version + 1)
            .where(MF_PLAYER.ID.eq(player.id.value))
            .and(MF_PLAYER.VERSION.eq(player.version))
            .execute()
        if (rowCount == 0) return@transactionResult null
        val stored = transactionalDsl.selectFrom(MF_PLAYER)
            .where(MF_PLAYER.ID.eq(player.id.value))
            .fetchOne()
            .let(::requireNotNull)
            .toDomain()
        // On MySQL and MariaDB, jOOQ emulates the guarded update with CASE expressions, and the
        // driver's default found-rows reporting counts a skipped update as one row, so rowCount
        // alone does not reveal a stale write there. The row is read back inside the same
        // transaction (the write holds its row lock), so a row that does not hold what was just
        // written means the write was skipped. The name is left out of the comparison: it is
        // display data refreshed on every login, and a database that truncates an over-long
        // name (`name` is varchar(16)) must not turn every save of that player into a conflict.
        return@transactionResult stored.takeIf { it.holdsValuesOf(player) }
    }

    private fun MfPlayer.holdsValuesOf(written: MfPlayer) =
        power == written.power &&
            powerAtLogout == written.powerAtLogout &&
            isBypassEnabled == written.isBypassEnabled &&
            chatChannel == written.chatChannel &&
            fixedPower == written.fixedPower

    override fun increaseOnlinePlayerPower(onlinePlayerIds: List<MfPlayerId>, leaderIds: Set<MfPlayerId>) {
        val maxPower = MfMaxPower.maxPower(plugin.config)
        val leaderMaxPower = MfMaxPower.leaderMaxPower(plugin.config)
        if (leaderMaxPower == null) {
            increasePower(onlinePlayerIds, maxPower)
            return
        }
        val (leaders, others) = onlinePlayerIds.partition { it in leaderIds }
        increasePower(others, maxPower)
        increasePower(leaders, leaderMaxPower)
    }

    private fun increasePower(onlinePlayerIds: List<MfPlayerId>, maxPower: Double) {
        if (onlinePlayerIds.isEmpty()) return
        val minPower = plugin.config.getDouble("players.minPower")
        val hoursToReachMax = plugin.config.getDouble("players.hoursToReachMaxPower")
        val timeIncrementHours = MfPowerUpdateSettings.intervalHours(plugin.config)
        dsl.update(MF_PLAYER)
            .set(
                MF_PLAYER.POWER,
                least(
                    value(maxPower),
                    greatest(
                        value(minPower),
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
                    )
                )
            )
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(MF_PLAYER.ID.`in`(onlinePlayerIds.map { it.value }))
            .and(MF_PLAYER.POWER.lt(maxPower))
            // A player whose power an admin has fixed (/power setfixed) does not gain power.
            .and(MF_PLAYER.FIXED_POWER.isNull)
            .execute()
    }

    override fun decreaseOfflinePlayerPower(onlinePlayerIds: List<MfPlayerId>, leaderIds: Set<MfPlayerId>) {
        val maxPower = MfMaxPower.maxPower(plugin.config)
        val leaderMaxPower = MfMaxPower.leaderMaxPower(plugin.config)
        val offline = MF_PLAYER.ID.notIn(onlinePlayerIds.map { it.value })
        if (leaderMaxPower == null) {
            decreasePower(offline, maxPower)
            return
        }
        val leaderIdValues = leaderIds.map { it.value }
        decreasePower(offline.and(MF_PLAYER.ID.notIn(leaderIdValues)), maxPower)
        if (leaderIdValues.isNotEmpty()) {
            decreasePower(offline.and(MF_PLAYER.ID.`in`(leaderIdValues)), leaderMaxPower)
        }
    }

    override fun clampPowerToMax(leaderIds: Set<MfPlayerId>) {
        val leaderIdValues = leaderIds.map { it.value }
        clampPower(MF_PLAYER.ID.notIn(leaderIdValues), MfMaxPower.maxPower(plugin.config))
        val leaderMaxPower = MfMaxPower.leaderMaxPower(plugin.config) ?: return
        if (leaderIdValues.isNotEmpty()) {
            clampPower(MF_PLAYER.ID.`in`(leaderIdValues), leaderMaxPower)
        }
    }

    private fun clampPower(players: Condition, maxPower: Double) {
        dsl.update(MF_PLAYER)
            .set(MF_PLAYER.POWER, maxPower)
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(players)
            .and(MF_PLAYER.POWER.gt(maxPower))
            // A fixed power (/power setfixed, #1682) never changes
            .and(MF_PLAYER.FIXED_POWER.isNull)
            .execute()
    }

    private fun decreasePower(players: Condition, maxPower: Double) {
        val minPower = plugin.config.getDouble("players.minPower")
        val hoursToReachMin = plugin.config.getDouble("players.hoursToReachMinPower")
        val timeIncrementHours = MfPowerUpdateSettings.intervalHours(plugin.config)
        dsl.update(MF_PLAYER)
            .set(
                MF_PLAYER.POWER,
                least(
                    value(maxPower),
                    greatest(
                        value(minPower),
                        // Determine the current time by running the inverse
                        least(MF_PLAYER.POWER, MF_PLAYER.POWER_AT_LOGOUT, maxPower)
                            .minus(minPower)
                            .div(MF_PLAYER.POWER_AT_LOGOUT.minus(minPower))
                            .minus(1)
                            .div(-1)
                            .pow(0.25)
                            .times(hoursToReachMin)
                            // Add the time increment to the time value we've determined
                            .plus(timeIncrementHours)
                            // Run the formula normally on the new time to determine the new power value
                            .div(hoursToReachMin) // extend the graph from reaching the minimum value at 1 to hoursToReachMin
                            .pow(4) // exp4 curve
                            .times(-1) // flip the graph (exp4 is normally U-shaped)
                            .plus(1) // shift the graph up above the x axis (it will usually peak at the minimum value)
                            .times(MF_PLAYER.POWER_AT_LOGOUT.minus(minPower)) // scale the graph to the range of the power values
                            .plus(minPower) // shift the graph back down to the minimum power value
                    )
                )
            )
            .set(MF_PLAYER.VERSION, MF_PLAYER.VERSION.plus(1))
            .where(players)
            .and(MF_PLAYER.POWER_AT_LOGOUT.gt(minPower))
            .and(MF_PLAYER.POWER.gt(minPower))
            // A player whose power an admin has fixed (/power setfixed) does not lose power.
            .and(MF_PLAYER.FIXED_POWER.isNull)
            .execute()
    }

    private fun MfPlayerRecord.toDomain() = MfPlayer(
        MfPlayerId(id),
        version,
        name,
        power,
        powerAtLogout,
        bypassEnabled,
        chatChannel?.let(MfFactionChatChannel::valueOf),
        fixedPower
    )
}
