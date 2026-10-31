package ru.aokazantsev.devatlas.testing

import ru.aokazantsev.devatlas.journal.domain.AppJournal

class FakeJournal(var tailText: String = "") : AppJournal {
    val lines = mutableListOf<String>()

    override fun write(message: String) {
        lines += message
    }

    override fun tail(maxBytes: Int): String = tailText
}
