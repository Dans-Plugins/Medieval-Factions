package com.dansplugins.factionsystem.update

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class MfUpdateCheckTest {

    private val url = "https://github.com/Dans-Plugins/Medieval-Factions/releases/tag/v7.0.0"
    private val latest = MfLatestRelease("v7.0.0", url)

    @Test
    fun testDisabledReasonCombinations() {
        assertNull(MfUpdateCheck.disabledReason(configEnabled = true, environmentOptsOut = false))
        assertEquals(MfUpdateCheck.REASON_CONFIG, MfUpdateCheck.disabledReason(configEnabled = false, environmentOptsOut = false))
        assertEquals(MfUpdateCheck.REASON_ENVIRONMENT, MfUpdateCheck.disabledReason(configEnabled = true, environmentOptsOut = true))
        // the environment wins over the config, as for usage reporting
        assertEquals(MfUpdateCheck.REASON_ENVIRONMENT, MfUpdateCheck.disabledReason(configEnabled = false, environmentOptsOut = true))
    }

    @Test
    fun testOlderReleaseGetsNotice() {
        val notice = assertNotNull(MfUpdateCheck.noticeFor("6.1.0", latest))
        assertEquals("7.0.0", notice.latestVersion)
        assertEquals("6.1.0", notice.currentVersion)
        assertEquals(url, notice.url)
        assertEquals(MfUpdateNotice.Reason.GENERIC, notice.reason)
    }

    @Test
    fun testPre600GetsH2Reason() {
        val notice = assertNotNull(MfUpdateCheck.noticeFor("5.8.1", latest))
        assertEquals(MfUpdateNotice.Reason.H2_SHUTDOWN_CORRUPTION, notice.reason)
        assertTrue(notice.consoleMessage().contains("database corruption on shutdown (H2)"))
        assertTrue(notice.consoleMessage().contains(url))
    }

    @Test
    fun test600PreReleaseGetsH2Reason() {
        // 6.0.0-SNAPSHOT predates the 6.0.0 release that closes H2 cleanly
        val notice = assertNotNull(MfUpdateCheck.noticeFor("6.0.0-SNAPSHOT", latest))
        assertEquals(MfUpdateNotice.Reason.H2_SHUTDOWN_CORRUPTION, notice.reason)
    }

    @Test
    fun testSameVersionGetsNoNotice() {
        assertNull(MfUpdateCheck.noticeFor("7.0.0", latest))
    }

    @Test
    fun testNewerSnapshotIsNotToldToDowngrade() {
        assertNull(MfUpdateCheck.noticeFor("7.0.1-SNAPSHOT", latest))
    }

    @Test
    fun testSnapshotOfTheReleasedVersionIsToldToUpgrade() {
        assertNotNull(MfUpdateCheck.noticeFor("7.0.0-SNAPSHOT", latest))
    }

    @Test
    fun testNewerReleaseIsNotToldToDowngrade() {
        assertNull(MfUpdateCheck.noticeFor("7.1.0", latest))
    }

    @Test
    fun testUnrecognisedVersionsOrNoReleaseGetNoNotice() {
        assertNull(MfUpdateCheck.noticeFor("6.1.0", null))
        assertNull(MfUpdateCheck.noticeFor("6.1.0", MfLatestRelease("dev", url)))
        assertNull(MfUpdateCheck.noticeFor("@version@", latest))
    }

    @Test
    fun testPreReleaseTagIsNotOffered() {
        assertNull(MfUpdateCheck.noticeFor("6.1.0", MfLatestRelease("v7.1.0-rc.1", url)))
    }

    @Test
    fun testParseReleaseJson() {
        val release = MfGitHubReleaseFetcher.parse(
            """{"tag_name":"v7.0.0","name":"7.0.0","html_url":"$url","draft":false,"prerelease":false,"body":"## 7.0.0"}"""
        )
        assertEquals(latest, release)
    }

    @Test
    fun testParseReleaseJsonRejectsPreReleasesAndJunk() {
        assertNull(MfGitHubReleaseFetcher.parse("""{"tag_name":"dev","html_url":"$url","prerelease":true}"""))
        assertNull(MfGitHubReleaseFetcher.parse("""{"tag_name":"v7.0.0","draft":true}"""))
        assertNull(MfGitHubReleaseFetcher.parse("""{"message":"API rate limit exceeded"}"""))
        assertNull(MfGitHubReleaseFetcher.parse("""[]"""))
    }

    @Test
    fun testParseReleaseJsonFallsBackToTagUrl() {
        val release = assertNotNull(MfGitHubReleaseFetcher.parse("""{"tag_name":"v7.0.0"}"""))
        assertEquals(url, release.url)
    }

    @Test
    fun testFetcherFailureIsSilentNull() {
        // nothing listens on port 1 of the loopback address, so the connection is refused at once
        val fetcher = MfGitHubReleaseFetcher("test", null, "http://127.0.0.1:1/releases/latest")
        assertNull(fetcher())
    }
}
