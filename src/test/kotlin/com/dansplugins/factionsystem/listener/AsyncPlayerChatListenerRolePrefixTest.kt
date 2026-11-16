package com.dansplugins.factionsystem.listener

import com.dansplugins.factionsystem.MedievalFactions
import net.md_5.bungee.api.ChatColor
import org.bukkit.configuration.file.FileConfiguration
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

/**
 * How a role prefix (#1525) goes into the public chat format that `chat.enableDefaultChatFormatting` sets.
 */
class AsyncPlayerChatListenerRolePrefixTest {
    private val uut = AsyncPlayerChatListener(
        mock(MedievalFactions::class.java).also { plugin ->
            `when`(plugin.config).thenReturn(mock(FileConfiguration::class.java))
        }
    )

    @Test
    fun `a role without a prefix adds nothing, so the format is unchanged`() {
        assertEquals("", uut.rolePrefixFormat(null))
        assertEquals("", uut.rolePrefixFormat(""))
    }

    @Test
    fun `a prefix is followed by a colour reset and a space`() {
        assertEquals("[Knight]${ChatColor.WHITE} ", uut.rolePrefixFormat("[Knight]"))
    }

    @Test
    fun `a percent sign in a prefix is escaped so the format still formats`() {
        val format = "${uut.rolePrefixFormat("[100%]")}%s: %s"
        assertEquals("[100%]${ChatColor.WHITE} Alice: hi", String.format(format, "Alice", "hi"))
    }
}
