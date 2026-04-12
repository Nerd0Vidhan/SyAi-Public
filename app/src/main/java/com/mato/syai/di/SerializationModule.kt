package com.mato.syai.di

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mato.syai.note.data.local.parser.ObjectPayloadAdapter
import com.mato.syai.note.domain.local.model.ObjectPayload
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SerializationModule {

    /*@Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .serializeNulls()
            .create()
    }*/
}