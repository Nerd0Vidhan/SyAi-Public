package com.mato.syai.note.di

import android.content.Context
import androidx.room.Room
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.mato.syai.note.data.local.database.NoteDao
import com.mato.syai.note.data.local.database.NoteDatabase
import com.mato.syai.note.data.local.parser.ObjectPayloadAdapter
import com.mato.syai.note.data.local.repository.NoteRepository
import com.mato.syai.note.data.local.security.CryptoManager
import com.mato.syai.note.domain.local.model.ObjectPayload
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NoteModule {

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
    fun provideDao(db: NoteDatabase): NoteDao = db.noteDao()


    @Provides
    @Singleton
    fun provideGson(): Gson {
        return GsonBuilder()
            .registerTypeAdapter(ObjectPayload::class.java, ObjectPayloadAdapter())
            .create()
    }
}