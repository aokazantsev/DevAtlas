package ru.aokazantsev.devatlas.update.data

import ru.aokazantsev.devatlas.update.domain.model.AppVersion
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReleasesParserTest {

    @Test
    fun takesPublishedReleasesWithApkAndDigest() {
        val list = ReleasesParser.parse(
            """
            [
              {"tag_name": "v0.3", "draft": true, "prerelease": false, "body_html": "", "assets": []},
              {"tag_name": "v0.3-beta", "draft": false, "prerelease": true, "body_html": "", "assets": []},
              {"tag_name": "v0.2", "draft": false, "prerelease": false, "body_html": "<p>Новое</p>", "assets": [
                {"name": "DevAtlas-0.2.apk.sha256", "browser_download_url": "https://x/sha", "size": 64},
                {"name": "DevAtlas-0.2.apk", "browser_download_url": "https://x/apk", "size": 1024,
                 "digest": "sha256:${"ab".repeat(32)}"}
              ]},
              {"tag_name": "nightly", "draft": false, "prerelease": false, "body_html": "", "assets": []}
            ]
            """.trimIndent(),
        )!!

        val release = list.releases.single()
        assertEquals(AppVersion(listOf(0, 2)), release.version)
        assertEquals("<p>Новое</p>", release.bodyHtml)
        assertEquals("https://x/apk", release.apk?.url)
        assertEquals(1024L, release.apk?.sizeBytes)
        assertEquals("ab".repeat(32), release.apk?.sha256)
        assertTrue(list.complete)
    }

    @Test
    fun foreignDigestIsNotTrusted() {
        val list = ReleasesParser.parse(
            """[{"tag_name": "v1.0", "assets": [{"name": "a.apk", "browser_download_url": "https://x/a", "digest": "md5:00"}]}]""",
        )!!

        assertNull(list.releases.single().apk?.sha256)
    }

    @Test
    fun fullPageMeansThereAreOlderReleases() {
        val items = (1..ReleasesParser.PAGE_SIZE).joinToString(",") { """{"tag_name": "v0.$it"}""" }

        assertFalse(ReleasesParser.parse("[$items]")!!.complete)
    }

    @Test
    fun notAListIsRejected() {
        assertNull(ReleasesParser.parse("""{"message": "rate limit"}"""))
        assertNull(ReleasesParser.parse("<html>"))
    }
}
