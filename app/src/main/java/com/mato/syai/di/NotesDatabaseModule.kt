package com.mato.syai.di

import android.content.Context
import androidx.room.Room
import com.mato.syai.notes.data.local.NotesDao
import com.mato.syai.notes.data.local.NotesDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NotesDatabaseModule {

    @Provides
    @Singleton
    fun provideNotesDatabase(
        @ApplicationContext context: Context
    ): NotesDatabase {

        return Room.databaseBuilder(
            context,
            NotesDatabase::class.java,
            "syai_notes_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNotesDao(
        database: NotesDatabase
    ): NotesDao {
        return database.notesDao()
    }
}