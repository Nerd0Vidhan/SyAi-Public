package com.mato.syai.di

import com.mato.syai.note.data.local.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideCryptoManager() = CryptoManager()

    @Provides
    fun provideThumbnailUtils() = com.mato.syai.note.utils.ThumbnailUtils
}