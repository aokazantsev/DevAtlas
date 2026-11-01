package ru.aokazantsev.devatlas.e2e

import mockwebserver3.MockResponse
import okio.Buffer
import ru.aokazantsev.devatlas.BuildConfig
import java.security.MessageDigest

object Releases {

    const val APK_PATH = "/releases/DevAtlas-9.9.apk"
    const val NEW_VERSION = "9.9"
    val INSTALLED_VERSION = BuildConfig.VERSION_NAME.substringBefore('-')
    const val CHANGE_TEXT = "Исправлена прокрутка длинных страниц"
    val APK_BYTES = "e2e apk bytes".toByteArray()

    fun serve(
        server: MockServerRule,
        sha256: String? = sha256(APK_BYTES),
        versions: List<String> = listOf(NEW_VERSION, INSTALLED_VERSION),
        extraNotesHtml: String = "",
    ) {
        val items = versions.joinToString(",") { version -> release(server, version, sha256, extraNotesHtml) }
        server.route(TestNetwork.RELEASES_PATH) {
            MockResponse.Builder()
                .code(MockServerRule.OK)
                .addHeader("Content-Type", "application/json")
                .body("[$items]")
                .build()
        }
        server.route(APK_PATH) {
            MockResponse.Builder()
                .code(MockServerRule.OK)
                .addHeader("Content-Type", "application/vnd.android.package-archive")
                .body(Buffer().write(APK_BYTES))
                .build()
        }
    }

    private fun release(server: MockServerRule, version: String, sha256: String?, extraNotesHtml: String): String {
        val digest = sha256?.let { ", \"digest\": \"sha256:$it\"" }.orEmpty()
        return """
        {"tag_name": "v$version", "draft": false, "prerelease": false,
         "body_html": "<h2>Что исправлено</h2><ul><li>$CHANGE_TEXT в версии $version</li></ul>$extraNotesHtml",
         "assets": [{"name": "DevAtlas-$version.apk", "size": ${APK_BYTES.size},
           "browser_download_url": "${server.baseUrl}$APK_PATH"$digest}]}
        """.trimIndent()
    }

    fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
