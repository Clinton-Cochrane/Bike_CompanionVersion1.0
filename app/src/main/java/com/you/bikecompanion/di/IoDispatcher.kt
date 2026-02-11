package com.you.bikecompanion.di

import javax.inject.Qualifier

/**
 * Qualifier for the coroutine dispatcher used for IO work (e.g. building context from DB).
 * In production bound to [kotlinx.coroutines.Dispatchers.IO]; in tests can be replaced with a test dispatcher.
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher
