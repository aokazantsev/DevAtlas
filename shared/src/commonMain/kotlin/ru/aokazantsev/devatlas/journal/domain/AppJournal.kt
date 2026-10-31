package ru.aokazantsev.devatlas.journal.domain

interface AppJournal {
    fun write(message: String)

    fun tail(maxBytes: Int): String
}
