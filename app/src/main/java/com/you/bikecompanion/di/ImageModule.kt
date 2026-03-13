package com.you.bikecompanion.di

import android.content.Context
import android.net.Uri
import com.you.bikecompanion.data.image.ImageRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import java.io.File
import java.io.InputStream
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object ImageModule {

    @Provides
    @Singleton
    fun provideImageRepository(
        @ApplicationContext context: Context,
    ): ImageRepository {
        val baseDir = File(context.filesDir, "images")
        val openUriStream: (Uri) -> InputStream? = { context.contentResolver.openInputStream(it) }
        return ImageRepository(baseDir, openUriStream)
    }
}
