package com.you.bikecompanion.di

import com.you.bikecompanion.ai.AiApiClient
import com.you.bikecompanion.ai.PlaceholderAiClient
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
    abstract fun bindAiApiClient(impl: PlaceholderAiClient): AiApiClient
}
