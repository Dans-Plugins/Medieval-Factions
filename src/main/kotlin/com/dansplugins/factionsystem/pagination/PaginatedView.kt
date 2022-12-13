package com.dansplugins.factionsystem.pagination

import com.dansplugins.factionsystem.lang.Language
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.ClickEvent.Action.RUN_COMMAND
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.HoverEvent.Action.SHOW_TEXT
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.command.CommandSender

class PaginatedView(
    private val language: Language,
    val title: Lazy<Array<out BaseComponent>>,
    lines: List<Lazy<Array<out BaseComponent>>>,
    pageLength: Int = 10,
    val viewPageCommand: (Int) -> String
) {

    val pages = lines.chunked(pageLength)

    companion object {
        fun fromLazyStrings(
            language: Language,
            title: Lazy<String>,
            lines: List<Lazy<String>>,
            pageLength: Int = 10,
            viewPageCommand: (Int) -> String
        ) = PaginatedView(
            language,
            lazy { TextComponent.fromLegacyText(title.value) },
            lines.map {
                lazy { TextComponent.fromLegacyText(it.value) }
            },
            pageLength,
            viewPageCommand
        )

        fun fromStrings(
            language: Language,
            title: String,
            lines: List<String>,
            pageLength: Int = 10,
            viewPageCommand: (Int) -> String
        ) = fromLazyStrings(language, lazy { title }, lines.map { lazy { it } }, pageLength, viewPageCommand)

        fun fromChatComponents(
            language: Language,
            title: Array<out BaseComponent>,
            lines: List<Array<out BaseComponent>>,
            pageLength: Int = 10,
            viewPageCommand: (Int) -> String
        ) = PaginatedView(language, lazy { title }, lines.map { lazy { it } }, pageLength, viewPageCommand)
    }

    fun sendPage(sender: CommandSender, pageNumber: Int = 0) {
        sender.spigot().sendMessage(*title.value)
        if (pageNumber in pages.indices) {
            val page = pages[pageNumber]
            page.forEach { line ->
                sender.spigot().sendMessage(*line.value)
            }
        }
        val pageControls = arrayOf(
            TextComponent(language["PreviousPage"]).apply {
                if (pageNumber - 1 >= 0) {
                    color = ChatColor.GREEN
                    clickEvent = ClickEvent(
                        RUN_COMMAND,
                        viewPageCommand(pageNumber - 1)
                    )
                    hoverEvent = HoverEvent(
                        SHOW_TEXT,
                        Text(language["PreviousPageHover"])
                    )
                } else {
                    color = ChatColor.DARK_GREEN
                }
            },
            TextComponent(" - ").apply {
                color = ChatColor.GRAY
            },
            TextComponent(language["Page", (pageNumber + 1).toString()]).apply {
                color = ChatColor.AQUA
            },
            TextComponent(" - ").apply {
                color = ChatColor.GRAY
            },
            TextComponent(language["NextPage"]).apply {
                if (pageNumber + 1 < pages.size) {
                    color = ChatColor.GREEN
                    clickEvent = ClickEvent(
                        RUN_COMMAND,
                        viewPageCommand(pageNumber + 1)
                    )
                    hoverEvent = HoverEvent(
                        SHOW_TEXT,
                        Text(language["NextPageHover"])
                    )
                } else {
                    color = ChatColor.DARK_GREEN
                }
            }
        )
        sender.spigot().sendMessage(*pageControls)
    }
}
