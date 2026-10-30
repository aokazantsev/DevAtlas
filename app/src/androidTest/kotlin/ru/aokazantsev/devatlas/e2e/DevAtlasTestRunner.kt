package ru.aokazantsev.devatlas.e2e

import android.app.Application
import android.content.Context
import androidx.test.runner.AndroidJUnitRunner

class DevAtlasTestRunner : AndroidJUnitRunner() {

    override fun newApplication(classLoader: ClassLoader, className: String, context: Context): Application =
        super.newApplication(classLoader, TestDevAtlasApplication::class.java.name, context)
}
