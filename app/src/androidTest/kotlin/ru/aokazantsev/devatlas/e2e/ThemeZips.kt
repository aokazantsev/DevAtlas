package ru.aokazantsev.devatlas.e2e

import android.content.res.AssetManager
import androidx.test.platform.app.InstrumentationRegistry
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

object ThemeZips {

    const val SAMPLE_ID = "devatlas-sample"
    const val SAMPLE_TITLE = "Пример DevAtlas"
    const val SAMPLE_PAGE_COUNT = 12
    const val EXTRA_PAGE_ID = "extra"
    const val REMOVED_PAGE_ID = "internal-links"
    const val CHANGED_PAGE_FILE = "pages/welcome.html"
    const val MANIFEST = "manifest.json"
    const val LINKS_PAGE_FILE = "pages/links/external.html"
    private const val WEB_LINKS_PAGE = """
        <h1>Сайты и PDF</h1>
        <p>Сайт: <a href="https://devatlas.test/web/article.html">статья на сайте</a>.</p>
        <p>Файл: <a href="https://devatlas.test/files/guide.pdf" download="e2e-guide.pdf">руководство в PDF</a>.</p>
        <p>Без расширения: <a href="https://devatlas.test/files/report">отчёт с сайта</a>.</p>
        <p>Медленный: <a href="https://devatlas.test/files/slow.pdf" download="e2e-slow.pdf">большой файл</a>.</p>
        <p>Битый: <a href="https://devatlas.test/files/missing.pdf" download="e2e-missing.pdf">пропавший файл</a>.</p>
        <p>Нет сети: <a href="https://offline.test/page.html">сайт без сети</a>.</p>
        <p>Без нажатия: <a href="https://devatlas.test/web/auto.html">сайт с автопереходом</a>.</p>
        <p>Почта: <a href="mailto:hello@example.com">написать письмо</a>, <a href="tel:+70000000000">позвонить</a>.</p>
    """

    fun sampleFiles(): Map<String, ByteArray> {
        val assets = InstrumentationRegistry.getInstrumentation().context.assets
        val files = linkedMapOf<String, ByteArray>()
        collect(assets, "sample-theme", "", files)
        return files
    }

    fun zip(files: Map<String, ByteArray>, rootFolder: String = ""): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            files.forEach { (name, bytes) ->
                zip.putNextEntry(ZipEntry(rootFolder + name))
                zip.write(bytes)
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }

    fun sampleZip(): ByteArray = zip(sampleFiles())

    fun sampleZipInRepositoryFolder(): ByteArray = zip(sampleFiles(), rootFolder = "repo-main/")

    fun sampleZipInRepositorySubfolder(subfolder: String): ByteArray = zip(sampleFiles(), rootFolder = "repo-main/$subfolder/")

    fun changedSampleFiles(): Map<String, ByteArray> {
        val files = sampleFiles().toMutableMap()
        files[CHANGED_PAGE_FILE] = files.getValue(CHANGED_PAGE_FILE) + "<p>Новый абзац после обновления.</p>\n".toByteArray()
        files["pages/extra.html"] = "<p>Страница, которой раньше не было.</p>\n".toByteArray()
        files.remove("pages/links/internal.html")
        val manifest = JSONObject(String(files.getValue(MANIFEST)))
        removeNode(manifest.getJSONArray("tree"), REMOVED_PAGE_ID)
        manifest.getJSONArray("tree").put(
            JSONObject().put("id", EXTRA_PAGE_ID).put("title", "Новая страница").put("file", "pages/extra.html"),
        )
        files[MANIFEST] = manifest.toString().toByteArray()
        return files
    }

    fun changedSampleZip(): ByteArray = zip(changedSampleFiles())

    fun withManifestId(id: String, title: String? = null): ByteArray {
        val files = sampleFiles().toMutableMap()
        val manifest = JSONObject(String(files.getValue(MANIFEST))).put("id", id)
        if (title != null) manifest.put("title", title)
        files[MANIFEST] = manifest.toString().toByteArray()
        return zip(files)
    }

    fun withBrokenWelcomePage(): ByteArray {
        val files = sampleFiles().toMutableMap()
        files[CHANGED_PAGE_FILE] = "<html><body><p>обёртка запрещена</p></body></html>".toByteArray()
        return zip(files)
    }

    fun withWebLinks(): ByteArray {
        val files = sampleFiles().toMutableMap()
        files[LINKS_PAGE_FILE] = WEB_LINKS_PAGE.trimIndent().toByteArray()
        return zip(files)
    }

    fun withoutManifest(): ByteArray = zip(sampleFiles().filterKeys { it != MANIFEST })

    fun withUnsafePath(): ByteArray = zip(sampleFiles() + ("../evil.html" to "x".toByteArray()))

    fun notAZip(): ByteArray = "<html>это не архив</html>".toByteArray()

    private fun removeNode(nodes: JSONArray, id: String) {
        var index = 0
        while (index < nodes.length()) {
            val node = nodes.getJSONObject(index)
            if (node.getString("id") == id) {
                nodes.remove(index)
            } else {
                node.optJSONArray("children")?.let { removeNode(it, id) }
                index++
            }
        }
    }

    private fun collect(assets: AssetManager, path: String, relative: String, into: MutableMap<String, ByteArray>) {
        val children = assets.list(path).orEmpty()
        if (children.isEmpty()) {
            into[relative] = assets.open(path).use { it.readBytes() }
            return
        }
        children.sorted().forEach { child ->
            collect(assets, "$path/$child", if (relative.isEmpty()) child else "$relative/$child", into)
        }
    }
}
