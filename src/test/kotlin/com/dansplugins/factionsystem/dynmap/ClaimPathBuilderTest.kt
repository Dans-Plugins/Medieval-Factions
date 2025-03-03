
import com.dansplugins.factionsystem.claim.MfClaimedChunk
import com.dansplugins.factionsystem.dynmap.Path
import com.dansplugins.factionsystem.dynmap.Point
import com.dansplugins.factionsystem.dynmap.builders.ClaimPathBuilder
import com.dansplugins.factionsystem.faction.MfFactionId
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.util.*

class ClaimPathBuilderTest {

    @Test
    fun testGetPaths_singleClaim() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(MfClaimedChunk(uuid, 0, 0, factionId))
        val paths = ClaimPathBuilder.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(0, 0), Point(1, 0), Point(1, 1), Point(0, 1))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_multipleClaims() {
        val uuid = UUID.randomUUID()
        val factionId = MfFactionId("factionId")
        val claims = listOf(
            MfClaimedChunk(uuid, 0, 0, factionId),
            MfClaimedChunk(uuid, 1, 0, factionId),
            MfClaimedChunk(uuid, 0, 1, factionId),
            MfClaimedChunk(uuid, 1, 1, factionId)
        )
        val paths = ClaimPathBuilder.getPaths(claims)
        val expectedPaths = listOf(
            listOf(Point(0, 0), Point(1, 0), Point(2, 0), Point(2, 1), Point(2, 2), Point(1, 2), Point(0, 2), Point(0, 1))
        )
        assertEquals(expectedPaths, paths)
    }

    @Test
    fun testGetPaths_noClaims() {
        val claims = emptyList<MfClaimedChunk>()
        val paths = ClaimPathBuilder.getPaths(claims)
        val expectedPaths = emptyList<Path>()
        assertEquals(expectedPaths, paths)
    }
}