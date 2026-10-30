package ru.aokazantsev.devatlas.ui.contents

data class ContentsProgress(val viewed: Int, val total: Int) {
    val fraction: Float
        get() = if (total == 0) 0f else viewed.toFloat() / total
}
