package com.clintoncochrane.bikecompanion

import android.app.Application
import java.io.File
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt uses this to provide application-scoped dependencies.
 */
@HiltAndroidApp
class BikeCompanionApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        File(filesDir, "images").deleteRecursively()
    }
}
