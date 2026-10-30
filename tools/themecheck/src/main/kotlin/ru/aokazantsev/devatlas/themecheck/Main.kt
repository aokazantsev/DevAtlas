package ru.aokazantsev.devatlas.themecheck

import java.io.FileDescriptor
import java.io.FileOutputStream
import java.io.PrintStream
import kotlin.system.exitProcess

fun main(args: Array<String>) {
    val output = if (System.console() != null) System.out else PrintStream(FileOutputStream(FileDescriptor.out), true, Charsets.UTF_8)
    val code = ThemeCheckCommand(output = output).run(args.toList())
    output.flush()
    exitProcess(code)
}
