package ru.aokazantsev.devatlas.e2e

object WebSite {
    const val ARTICLE_PATH = "/web/article.html"
    const val NEXT_PATH = "/web/next.html"
    const val GUIDE_PATH = "/files/guide.pdf"
    const val REPORT_PATH = "/files/report"
    const val SLOW_PATH = "/files/slow.pdf"
    const val MISSING_PATH = "/files/missing.pdf"
    const val MISSING_NAME = "e2e-missing.pdf"
    const val ATTACHMENT_PATH = "/files/attachment"
    const val ARTICLE_TITLE = "Статья для проверки"
    const val NEXT_TITLE = "Вторая страница"
    const val SITE_MAIL = "mailto:site@example.com"
    const val AUTO_PATH = "/web/auto.html"
    const val AUTO_TITLE = "Страница с автопереходом"
    const val AUTO_MAIL = "mailto:auto@example.com"
    const val GUIDE_NAME = "e2e-guide.pdf"
    const val REPORT_NAME = "report-from-site.pdf"
    const val ATTACHMENT_NAME = "attachment-name.pdf"
    const val GUIDE_URL = "${TestNetwork.WEB_ORIGIN}$GUIDE_PATH"
    const val ARTICLE_URL = "${TestNetwork.WEB_ORIGIN}$ARTICLE_PATH"
    private const val SLOW_SIZE = 64 * 1024

    fun serve(server: MockServerRule) {
        server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.withWebLinks())
        server.serveHtml(
            ARTICLE_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$ARTICLE_TITLE</title></head><body>" +
                "<script>document.addEventListener('click', function(e) { if (e.isTrusted) location.href = '$NEXT_PATH' })</script>" +
                "<h1>Статья</h1>" +
                "<p><a id=\"next\" href=\"$NEXT_PATH\">Дальше по сайту</a></p>" +
                "<p><a id=\"attachment\" href=\"$ATTACHMENT_PATH\">Вложение</a></p></body></html>",
        )
        server.serveHtml(
            NEXT_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$NEXT_TITLE</title></head><body>" +
                "<script>document.addEventListener('click', function(e) { if (e.isTrusted) location.href = '$SITE_MAIL' })</script>" +
                "<h1>Вторая</h1></body></html>",
        )
        server.serveHtml(
            AUTO_PATH,
            "<html><head><title>$AUTO_TITLE</title></head><body><h1>Авто</h1>" +
                "<script>setTimeout(function() { location.href = '$AUTO_MAIL' }, 500)</script></body></html>",
        )
        server.servePdf(GUIDE_PATH)
        server.servePdf(REPORT_PATH, fileName = REPORT_NAME)
        server.servePdf(ATTACHMENT_PATH, fileName = ATTACHMENT_NAME)
        server.servePdf(SLOW_PATH, bytes = ByteArray(SLOW_SIZE), throttled = true)
    }
}
