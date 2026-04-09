package com.mato.syai.note.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.mato.syai.note.data.local.database.NoteDao
import com.mato.syai.note.data.local.database.NoteDatabase
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.data.local.security.CryptoManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): NoteDatabase {
        return Room.databaseBuilder(
            context,
            NoteDatabase::class.java, // Point to the Database class, not the Entity
            "syai_notes_db"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideNoteDao(db: NoteDatabase): NoteDao {
        return db.noteDao()
    }

    // NoteRepository doesn't strictly need a @Provides if you use @Inject constructor
    // in the Repository class itself, but this is fine too.
    @Provides
    @Singleton
    fun provideRepository(dao: NoteDao,cryptoManager: CryptoManager,gson: Gson): NoteRepository {
        return NoteRepository(
            dao=dao,
            cryptoManager = cryptoManager,
            gson = gson
        )
    }
}