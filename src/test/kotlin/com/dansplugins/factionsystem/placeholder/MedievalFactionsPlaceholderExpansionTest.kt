
import com.dansplugins.factionsystem.MedievalFactions
import com.dansplugins.factionsystem.lang.Language
import com.dansplugins.factionsystem.placeholder.MedievalFactionsPlaceholderExpansion
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import java.util.*

class MedievalFactionsPlaceholderExpansionTest {

 private lateinit var plugin: MedievalFactions
 private lateinit var placeholderExpansion: MedievalFactionsPlaceholderExpansion
 private lateinit var language: Language

 @BeforeEach
 fun setUp() {
  // Mocking MedievalFactions plugin and Language
  plugin = mock(MedievalFactions::class.java)
  language = mock(Language::class.java)

  // Mock the plugin.name to avoid NullPointerException
  `when`(plugin.name).thenReturn("MedievalFactions")

  // Ensure language locale is non-null
  `when`(plugin.language).thenReturn(language)
  `when`(language.locale).thenReturn(Locale.ENGLISH)

  // Initialize the placeholder expansion
  placeholderExpansion = MedievalFactionsPlaceholderExpansion(plugin)
 }

 @Test
 fun testGetIdentifier() {
  val identifier = placeholderExpansion.identifier
  assertEquals("MedievalFactions", identifier)
 }
}