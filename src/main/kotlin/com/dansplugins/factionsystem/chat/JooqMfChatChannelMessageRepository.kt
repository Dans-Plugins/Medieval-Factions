package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.faction.MfFactionId
import com.dansplugins.factionsystem.jooq.Tables.MF_CHAT_CHANNEL_MESSAGE
import com.dansplugins.factionsystem.jooq.tables.records.MfChatChannelMessageRecord
import com.dansplugins.factionsystem.player.MfPlayerId
import org.jooq.DSLContext
import java.time.LocalDateTime
import java.time.ZoneOffset.UTC

class JooqMfChatChannelMessageRepository(private val dsl: DSLContext) : MfChatChannelMessageRepository {
    override fun insert(message: MfChatChannelMessage) {
        dsl.insertInto(MF_CHAT_CHANNEL_MESSAGE)
            .set(MF_CHAT_CHANNEL_MESSAGE.TIMESTAMP, LocalDateTime.ofInstant(message.timestamp, UTC))
            .set(MF_CHAT_CHANNEL_MESSAGE.PLAYER_ID, message.playerId.value)
            .set(MF_CHAT_CHANNEL_MESSAGE.FACTION_ID, message.factionId.value)
            .set(MF_CHAT_CHANNEL_MESSAGE.CHAT_CHANNEL, message.chatChannel.name)
            .set(MF_CHAT_CHANNEL_MESSAGE.MESSAGE, message.message)
            .execute()
    }

    override fun getChatChannelMessages(factionId: MfFactionId): List<MfChatChannelMessage> {
        return dsl.selectFrom(MF_CHAT_CHANNEL_MESSAGE)
            .where(MF_CHAT_CHANNEL_MESSAGE.FACTION_ID.eq(factionId.value))
            .orderBy(MF_CHAT_CHANNEL_MESSAGE.TIMESTAMP.desc())
            .fetch()
            .map { it.toDomain() }
    }

    override fun getChatChannelMessages(factionId: MfFactionId, limit: Int, offset: Int): List<MfChatChannelMessage> {
        return dsl.selectFrom(MF_CHAT_CHANNEL_MESSAGE)
            .where(MF_CHAT_CHANNEL_MESSAGE.FACTION_ID.eq(factionId.value))
            .orderBy(MF_CHAT_CHANNEL_MESSAGE.TIMESTAMP.desc())
            .limit(limit)
            .offset(offset)
            .fetch()
            .map { it.toDomain() }
    }

    override fun getChatChannelMessageCount(factionId: MfFactionId): Int {
        return dsl.selectCount().from(MF_CHAT_CHANNEL_MESSAGE)
            .where(MF_CHAT_CHANNEL_MESSAGE.FACTION_ID.eq(factionId.value))
            .fetchOne()
            ?.value1()
            ?: 0
    }

    private fun MfChatChannelMessageRecord.toDomain() = MfChatChannelMessage(
        timestamp.toInstant(UTC),
        playerId.let(::MfPlayerId),
        factionId.let(::MfFactionId),
        chatChannel.let(MfFactionChatChannel::valueOf),
        message
    )
}
