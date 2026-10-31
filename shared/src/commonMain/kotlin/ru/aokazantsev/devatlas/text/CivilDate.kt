package ru.aokazantsev.devatlas.text

data class CivilDate(val year: Int, val month: Int, val day: Int) {

    fun iso(): String = "$year-${month.twoDigits()}-${day.twoDigits()}"

    fun russian(): String = "${day.twoDigits()}.${month.twoDigits()}.$year"

    companion object {
        private const val MILLIS_PER_DAY = 86_400_000L
        private const val DAYS_FROM_YEAR_ZERO = 719_468L
        private const val DAYS_PER_ERA = 146_097L
        private const val YEARS_PER_ERA = 400L
        private const val DAYS_PER_YEAR = 365L
        private const val LEAP_EVERY = 4L
        private const val CENTURY = 100L
        private const val ERA_LAST_DAY = 146_096L
        private const val MONTH_STEPS = 153L
        private const val MONTH_STEP_OFFSET = 2L
        private const val MONTH_STEP_DIVISOR = 5L
        private const val MONTH_SHIFT = 3L
        private const val MONTHS = 12L
        private const val MARCH_SHIFT_LIMIT = 10L

        fun ofEpochMillis(epochMillis: Long): CivilDate {
            val days = epochMillis.floorDiv(MILLIS_PER_DAY) + DAYS_FROM_YEAR_ZERO
            val era = days.floorDiv(DAYS_PER_ERA)
            val dayOfEra = days - era * DAYS_PER_ERA
            val yearOfEra = (
                dayOfEra - dayOfEra / (DAYS_PER_YEAR * LEAP_EVERY) + dayOfEra / (DAYS_PER_YEAR * CENTURY + CENTURY / LEAP_EVERY - 1) -
                    dayOfEra / ERA_LAST_DAY
                ) / DAYS_PER_YEAR
            val dayOfYear = dayOfEra - (DAYS_PER_YEAR * yearOfEra + yearOfEra / LEAP_EVERY - yearOfEra / CENTURY)
            val shiftedMonth = (MONTH_STEP_DIVISOR * dayOfYear + MONTH_STEP_OFFSET) / MONTH_STEPS
            val day = dayOfYear - (MONTH_STEPS * shiftedMonth + MONTH_STEP_OFFSET) / MONTH_STEP_DIVISOR + 1
            val month = if (shiftedMonth < MARCH_SHIFT_LIMIT) shiftedMonth + MONTH_SHIFT else shiftedMonth - (MONTHS - MONTH_SHIFT)
            val year = yearOfEra + era * YEARS_PER_ERA + if (month <= 2) 1 else 0
            return CivilDate(year.toInt(), month.toInt(), day.toInt())
        }

        private fun Int.twoDigits(): String = toString().padStart(2, '0')
    }
}
