package ru.aokazantsev.devatlas.text

private const val TEN = 10
private const val HUNDRED = 100
private const val FIRST_TEEN = 11
private const val LAST_TEEN = 14
private const val FEW_FROM = 2
private const val FEW_TO = 4

fun russianPlural(count: Int, one: String, few: String, many: String): String {
    val lastDigit = count % TEN
    return when {
        count % HUNDRED in FIRST_TEEN..LAST_TEEN -> many
        lastDigit == 1 -> one
        lastDigit in FEW_FROM..FEW_TO -> few
        else -> many
    }
}
