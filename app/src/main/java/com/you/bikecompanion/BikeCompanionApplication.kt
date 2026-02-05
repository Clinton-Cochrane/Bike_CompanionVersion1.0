package com.you.bikecompanion

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

/**
 * Application entry point. Hilt uses this to provide application-scoped dependencies.
 */
@HiltAndroidApp
class BikeCompanionApplication : Application()
