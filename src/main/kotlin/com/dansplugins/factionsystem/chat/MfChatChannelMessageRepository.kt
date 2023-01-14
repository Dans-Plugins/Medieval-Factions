package com.dansplugins.factionsystem.chat

import com.dansplugins.factionsystem.faction.MfFactionId

interface MfChatChannelMessageRepository {

    fun insert(message: MfChatChannelMessage)
    fun getChatChannelMessages(factionId: MfFactionId): List<MfChatChannelMessage>
    fun getChatChannelMessages(factionId: MfFactionId, limit: Int, offset: Int = 0): List<MfChatChannelMessage>
    fun getChatChannelMessageCount(factionId: MfFactionId): Int
}
