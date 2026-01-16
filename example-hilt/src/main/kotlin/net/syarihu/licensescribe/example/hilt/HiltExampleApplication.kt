package net.syarihu.licensescribe.example.hilt

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Example application class with Hilt enabled.
 * This demonstrates that the generated LicenseScribeHiltModule works correctly with Hilt DI.
 */
@HiltAndroidApp
class HiltExampleApplication : Application()
