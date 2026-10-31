package ru.aokazantsev.devatlas.ui.web

import kotlin.test.Test
import kotlin.test.assertEquals

class WebHostTest {

    @Test
    fun hostIsTakenAfterUserInfo() {
        assertEquals("evil.example", WebHost.of("https://gosuslugi.ru@evil.example/login"))
    }

    @Test
    fun hostStopsAtPathQueryFragmentAndBackslash() {
        assertEquals("site.test", WebHost.of("https://site.test/a@b"))
        assertEquals("site.test", WebHost.of("https://site.test?next=a@b"))
        assertEquals("site.test", WebHost.of("https://site.test#a@b"))
        assertEquals("evil.example", WebHost.of("https://evil.example\\@gosuslugi.ru/"))
    }

    @Test
    fun portAndPunycodeAreShownAsIs() {
        assertEquals("xn--80ak6aa92e.com:8443", WebHost.of("https://xn--80ak6aa92e.com:8443/page"))
    }

    @Test
    fun urlWithoutHostIsShownWhole() {
        assertEquals("mailto:a@b.c", WebHost.of("mailto:a@b.c"))
        assertEquals("about:blank", WebHost.of("about:blank"))
    }
}
