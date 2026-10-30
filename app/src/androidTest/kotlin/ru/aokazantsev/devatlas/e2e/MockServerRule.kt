package ru.aokazantsev.devatlas.e2e

import mockwebserver3.MockWebServer
import org.junit.rules.ExternalResource

class MockServerRule : ExternalResource() {

    val server = MockWebServer()

    override fun before() {
        server.start()
        TestNetwork.baseUrl = server.url("/").toString()
    }

    override fun after() {
        TestNetwork.baseUrl = null
        server.close()
    }
}
