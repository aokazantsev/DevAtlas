package ru.aokazantsev.devatlas.usage.data

import ru.aokazantsev.devatlas.usage.data.model.UsageLedger

internal interface UsageLedgerStore {
    fun today(): String

    fun oldestKeptDay(): String

    fun load(): UsageLedger

    fun save(ledger: UsageLedger)

    fun clear()

    fun installId(): String
}
