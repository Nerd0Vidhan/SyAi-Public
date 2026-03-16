package com.mato.syai.di

import com.google.gson.Gson
import com.mato.syai.notes.data.local.NotesDao
import com.mato.syai.notes.data.repository.NotesRepositoryImpl
import com.mato.syai.notes.domain.repository.NotesRepository
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
object NotesRepositoryModule {

    @Provides
    fun provideNotesRepository(
        dao: NotesDao,
        gson: Gson
    ): NotesRepository {

        return NotesRepositoryImpl(
            dao = dao,
            gson = gson
        )
    }
}