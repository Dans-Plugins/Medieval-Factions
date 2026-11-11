package com.dansplugins.factionsystem.pagination

import com.dansplugins.factionsystem.lang.Language
import net.md_5.bungee.api.ChatColor
import net.md_5.bungee.api.chat.BaseComponent
import net.md_5.bungee.api.chat.ClickEvent
import net.md_5.bungee.api.chat.HoverEvent
import net.md_5.bungee.api.chat.TextComponent
import net.md_5.bungee.api.chat.hover.content.Text
import org.bukkit.command.CommandSender
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.mockingDetails
import org.mockito.Mockito.`when`

class PaginatedViewTest {
    private lateinit var language: Language
    private lateinit var sender: CommandSender
    private lateinit var spigot: CommandSender.Spigot

    @BeforeEach
    fun setUp() {
        language = mock(Language::class.java)
        `when`(language["PreviousPage"]).thenReturn("Previous")
        `when`(language["PreviousPageHover"]).thenReturn("Go back")
        `when`(language["NextPage"]).thenReturn("Next")
        `when`(language["NextPageHover"]).thenReturn("Go forward")
        for (page in 1..5) {
            `when`(language["Page", page.toString()]).thenReturn("Page $page")
        }
        sender = mock(CommandSender::class.java)
        spigot = mock(CommandSender.Spigot::class.java)
        `when`(sender.spigot()).thenReturn(spigot)
    }

    @Test
    fun pagesAreChunkedByPageLength() {
        val view = view((1..25).map { "line $it" }, pageLength = 10)

        assertEquals(3, view.pages.size)
        assertEquals(listOf(10, 10, 5), view.pages.map { it.size })
    }

    @Test
    fun pageLengthDefaultsToTen() {
        val view = PaginatedView.fromStrings(language, "Title", (1..11).map { "line $it" }) { "/cmd $it" }

        assertEquals(listOf(10, 1), view.pages.map { it.size })
    }

    @Test
    fun noLinesProducesNoPages() {
        val view = view(emptyList())

        assertTrue(view.pages.isEmpty())
    }

    @Test
    fun sendPageSendsTitleThenThatPagesLinesThenControls() {
        val view = view((1..5).map { "line $it" }, pageLength = 2)

        view.sendPage(sender, 1)

        val messages = sentMessages()
        assertEquals(listOf("Title", "line 3", "line 4"), messages.dropLast(1).map { plain(it) })
        assertEquals("Previous - Page 2 - Next", plain(messages.last()))
    }

    @Test
    fun sendPageDefaultsToTheFirstPage() {
        val view = view((1..3).map { "line $it" }, pageLength = 2)

        view.sendPage(sender)

        val messages = sentMessages()
        assertEquals(listOf("Title", "line 1", "line 2"), messages.dropLast(1).map { plain(it) })
        assertEquals("Page 1", plain(messages.last()[2]))
    }

    @Test
    fun firstPageHasInactivePreviousAndActiveNext() {
        val view = view((1..3).map { "line $it" }, pageLength = 2)

        view.sendPage(sender, 0)

        val controls = sentMessages().last()
        val previous = controls[0]
        assertEquals(ChatColor.DARK_GREEN, previous.color)
        assertNull(previous.clickEvent)
        assertNull(previous.hoverEvent)

        val next = controls[4]
        assertEquals(ChatColor.GREEN, next.color)
        assertEquals(ClickEvent.Action.RUN_COMMAND, next.clickEvent.action)
        assertEquals("/cmd 1", next.clickEvent.value)
        assertEquals(HoverEvent.Action.SHOW_TEXT, next.hoverEvent.action)
        assertEquals("Go forward", hoverText(next))
    }

    @Test
    fun lastPageHasActivePreviousAndInactiveNext() {
        val view = view((1..3).map { "line $it" }, pageLength = 2)

        view.sendPage(sender, 1)

        val controls = sentMessages().last()
        val previous = controls[0]
        assertEquals(ChatColor.GREEN, previous.color)
        assertEquals("/cmd 0", previous.clickEvent.value)
        assertEquals(HoverEvent.Action.SHOW_TEXT, previous.hoverEvent.action)
        assertEquals("Go back", hoverText(previous))

        val next = controls[4]
        assertEquals(ChatColor.DARK_GREEN, next.color)
        assertNull(next.clickEvent)
        assertNull(next.hoverEvent)
    }

    @Test
    fun controlSeparatorsAndPageLabelAreColoured() {
        val view = view((1..3).map { "line $it" }, pageLength = 2)

        view.sendPage(sender, 0)

        val controls = sentMessages().last()
        assertEquals(5, controls.size)
        assertEquals(ChatColor.GRAY, controls[1].color)
        assertEquals(ChatColor.AQUA, controls[2].color)
        assertEquals(ChatColor.GRAY, controls[3].color)
    }

    @Test
    fun outOfRangePageSendsOnlyTitleAndControls() {
        val view = view((1..3).map { "line $it" }, pageLength = 2)

        view.sendPage(sender, 4)

        val messages = sentMessages()
        assertEquals(2, messages.size)
        assertEquals("Title", plain(messages[0]))
        val controls = messages[1]
        assertEquals("Page 5", plain(controls[2]))
        // Previous points one page back from the requested page, even though that page is also out of range.
        assertEquals("/cmd 3", controls[0].clickEvent.value)
        assertNull(controls[4].clickEvent)
    }

    @Test
    fun emptyViewSendsTitleAndTwoInactiveControls() {
        val view = view(emptyList())

        view.sendPage(sender, 0)

        val messages = sentMessages()
        assertEquals(2, messages.size)
        val controls = messages[1]
        assertNull(controls[0].clickEvent)
        assertNull(controls[4].clickEvent)
    }

    @Test
    fun lazyLinesAreOnlyEvaluatedForTheSentPage() {
        val evaluated = mutableListOf<Int>()
        val lines = (1..4).map { index -> lazy { evaluated.add(index); "line $index" } }
        var titleEvaluations = 0
        val view = PaginatedView.fromLazyStrings(
            language,
            lazy { titleEvaluations++; "Title" },
            lines,
            pageLength = 2
        ) { "/cmd $it" }

        assertTrue(evaluated.isEmpty())
        assertEquals(0, titleEvaluations)

        view.sendPage(sender, 1)

        assertEquals(listOf(3, 4), evaluated)
        assertEquals(1, titleEvaluations)
    }

    @Test
    fun fromStringsParsesLegacyColourCodes() {
        val view = PaginatedView.fromStrings(language, "${org.bukkit.ChatColor.RED}Title", listOf("plain")) { "/cmd $it" }

        view.sendPage(sender, 0)

        val title = sentMessages()[0]
        assertEquals("Title", plain(title))
        assertTrue(title.any { it.color == ChatColor.RED })
    }

    @Test
    fun fromChatComponentsSendsTheGivenComponents() {
        val title = arrayOf<BaseComponent>(TextComponent("Title"))
        val line = arrayOf<BaseComponent>(TextComponent("first"), TextComponent("second"))
        val view = PaginatedView.fromChatComponents(language, title, listOf(line)) { "/cmd $it" }

        view.sendPage(sender, 0)

        val messages = sentMessages()
        assertTrue(messages[0][0] === title[0])
        assertEquals(2, messages[1].size)
        assertTrue(messages[1][0] === line[0])
        assertTrue(messages[1][1] === line[1])
        assertFalse(view.pages.isEmpty())
    }

    private fun view(lines: List<String>, pageLength: Int = 10) =
        PaginatedView.fromStrings(language, "Title", lines, pageLength) { "/cmd $it" }

    private fun sentMessages(): List<List<BaseComponent>> =
        mockingDetails(spigot).invocations
            .filter { it.method.name == "sendMessage" }
            .map { invocation -> invocation.arguments.map { it as BaseComponent } }

    private fun plain(components: List<BaseComponent>) = components.joinToString("") { it.toPlainText() }

    private fun plain(component: BaseComponent) = component.toPlainText()

    private fun hoverText(component: BaseComponent) =
        component.hoverEvent.contents.joinToString("") { (it as Text).value.toString() }
}
