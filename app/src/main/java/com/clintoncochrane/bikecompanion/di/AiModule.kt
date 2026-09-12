package com.clintoncochrane.bikecompanion.di

import com.clintoncochrane.bikecompanion.ai.AiApiClient
import com.clintoncochrane.bikecompanion.ai.GeminiApiClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AiModule {

    @Binds
    @Singleton
    abstract fun bindAiApiClient(impl: GeminiApiClient): AiApiClient
}
