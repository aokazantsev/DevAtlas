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
    const val AUTO_PDF_PATH = "/web/auto-pdf.html"
    const val AUTO_PDF_TITLE = "Страница с PDF без нажатия"
    const val PDF_TAP_PATH = "/web/pdf-tap.html"
    const val PDF_TAP_TITLE = "Страница со ссылкой на PDF"
    const val ATTACHMENT_TAP_PATH = "/web/attachment-tap.html"
    const val ATTACHMENT_TAP_TITLE = "Страница со ссылкой на вложение"
    const val SCROLL_PDF_PATH = "/web/scroll-pdf.html"
    const val SCROLL_PDF_TITLE = "Страница с PDF при прокрутке"
    const val PDF_THEN_SCRIPT_PATH = "/web/pdf-then-script.html"
    const val PDF_THEN_SCRIPT_TITLE = "Страница с PDF и загрузкой скриптом"
    const val SLOW_TAP_PATH = "/web/slow-tap.html"
    const val SLOW_TAP_TITLE = "Страница с медленным PDF"
    const val SLOW_ATTACHMENT_PATH = "/files/slow-attachment"
    const val SLOW_ATTACHMENT_NAME = "slow-attachment.pdf"
    const val DOWNLOAD_LINK_PATH = "/web/download-link.html"
    const val DOWNLOAD_LINK_TITLE = "Страница со ссылкой на скачивание"
    const val DOWNLOAD_LINK_TEXT = "файл вложения"
    const val PDF_REDIRECT_PATH = "/files/go"
    const val ATTACHMENT_REDIRECT_PATH = "/files/go-attachment"
    const val PDF_REDIRECT_TAP_PATH = "/web/pdf-redirect-tap.html"
    const val PDF_REDIRECT_TAP_TITLE = "Страница со ссылкой через переадресацию"
    const val ATTACHMENT_REDIRECT_TAP_PATH = "/web/attachment-redirect-tap.html"
    const val ATTACHMENT_REDIRECT_TAP_TITLE = "Страница с вложением через переадресацию"
    const val ORDINARY_TAP_PATH = "/web/ordinary-tap.html"
    const val ORDINARY_TAP_TITLE = "Страница со ссылкой на обычную страницу"
    const val UNTITLED_PATH = "/web/untitled.html"
    const val UNTITLED_HEADING = "Страница без заголовка"
    const val SLOW_HEADERS_SECONDS = 8L
    const val GUIDE_NAME = "e2e-guide.pdf"
    const val GUIDE_FILE_NAME = "guide.pdf"
    const val REPORT_NAME = "report-from-site.pdf"
    const val ATTACHMENT_NAME = "attachment-name.pdf"
    const val GUIDE_URL = "${TestNetwork.WEB_ORIGIN}$GUIDE_PATH"
    const val HOST = "devatlas.test"
    const val ARTICLE_URL = "${TestNetwork.WEB_ORIGIN}$ARTICLE_PATH"
    private const val SLOW_SIZE = 64 * 1024

    val servedHost: String
        get() = checkNotNull(TestNetwork.baseUrl).substringAfter("://").trimEnd('/')

    fun serve(server: MockServerRule) {
        server.serveZip(TestNetwork.SAMPLE_PATH, ThemeZips.withWebLinks())
        server.serveHtml(
            ARTICLE_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$ARTICLE_TITLE</title></head><body>" +
                "<script>document.addEventListener('click', function(e) { if (e.isTrusted) location.href = '$NEXT_PATH' })</script>" +
                "<h1>Статья</h1>" +
                "<p><a id=\"next\" href=\"$NEXT_PATH\">Дальше по сайту</a></p></body></html>",
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
        server.serveHtml(
            AUTO_PDF_PATH,
            "<html><head><title>$AUTO_PDF_TITLE</title></head><body><h1>PDF сам</h1><script>" +
                "setTimeout(function() { location.href = '$GUIDE_PATH' }, 300);" +
                "setTimeout(function() { location.href = '$ATTACHMENT_PATH' }, 900)</script></body></html>",
        )
        server.serveHtml(
            SCROLL_PDF_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$SCROLL_PDF_TITLE</title></head>" +
                "<body style=\"height: 6000px\"><h1>Листайте</h1><script>" +
                "window.addEventListener('scroll', function() { location.href = '$ATTACHMENT_PATH' })</script></body></html>",
        )
        server.serveHtml(
            PDF_THEN_SCRIPT_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$PDF_THEN_SCRIPT_TITLE</title></head><body>" +
                "<script>document.addEventListener('click', function(e) {" +
                " if (!e.isTrusted) return;" +
                " location.href = '$GUIDE_PATH';" +
                " setTimeout(function() { var a = document.createElement('a'); a.href = '$ATTACHMENT_PATH';" +
                " a.download = '$ATTACHMENT_NAME'; document.body.appendChild(a); a.click() }, 500) })</script>" +
                "<h1>Нажмите</h1></body></html>",
        )
        server.serveHtml(
            DOWNLOAD_LINK_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$DOWNLOAD_LINK_TITLE</title></head><body>" +
                "<h1>Скачать</h1><p><a id=\"file\" href=\"$ATTACHMENT_PATH\" download=\"$ATTACHMENT_NAME\">$DOWNLOAD_LINK_TEXT</a></p>" +
                "<script>document.getElementById('file').focus()</script></body></html>",
        )
        serveTapPage(server, SLOW_TAP_PATH, SLOW_TAP_TITLE, SLOW_ATTACHMENT_PATH)
        serveTapPage(server, PDF_TAP_PATH, PDF_TAP_TITLE, GUIDE_PATH)
        serveTapPage(server, ATTACHMENT_TAP_PATH, ATTACHMENT_TAP_TITLE, ATTACHMENT_PATH)
        serveRedirectsAndUntitled(server)
        server.servePdf(GUIDE_PATH)
        server.servePdf(REPORT_PATH, fileName = REPORT_NAME)
        server.servePdf(ATTACHMENT_PATH, fileName = ATTACHMENT_NAME)
        server.servePdf(SLOW_ATTACHMENT_PATH, fileName = SLOW_ATTACHMENT_NAME, headersDelaySeconds = SLOW_HEADERS_SECONDS)
        server.servePdf(SLOW_PATH, bytes = ByteArray(SLOW_SIZE), throttled = true)
    }

    fun servedUrl(path: String): String = "${checkNotNull(TestNetwork.baseUrl).trimEnd('/')}$path"

    private fun serveRedirectsAndUntitled(server: MockServerRule) {
        serveTapPage(server, PDF_REDIRECT_TAP_PATH, PDF_REDIRECT_TAP_TITLE, PDF_REDIRECT_PATH)
        serveTapPage(server, ATTACHMENT_REDIRECT_TAP_PATH, ATTACHMENT_REDIRECT_TAP_TITLE, ATTACHMENT_REDIRECT_PATH)
        serveTapPage(server, ORDINARY_TAP_PATH, ORDINARY_TAP_TITLE, AUTO_PDF_PATH)
        server.serveRedirect(PDF_REDIRECT_PATH, GUIDE_PATH)
        server.serveRedirect(ATTACHMENT_REDIRECT_PATH, ATTACHMENT_PATH)
        server.serveHtml(
            UNTITLED_PATH,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"></head><body><h1>$UNTITLED_HEADING</h1></body></html>",
        )
    }

    private fun serveTapPage(server: MockServerRule, path: String, title: String, target: String) {
        server.serveHtml(
            path,
            "<html><head><meta name=\"viewport\" content=\"width=device-width\"><title>$title</title></head><body>" +
                "<script>document.addEventListener('click', function(e) { if (e.isTrusted) location.href = '$target' })</script>" +
                "<h1>Нажмите</h1></body></html>",
        )
    }
}
