// AppModule.kt
package com.mato.syai.di

import android.content.Context
import androidx.datastore.dataStore
import com.google.firebase.auth.FirebaseAuth
import com.mato.syai.auth.AuthRepository
import com.mato.syai.auth.FirebaseAuthRepository
import com.mato.syai.ui.theme.ColorPreferencesSerializer
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AuthModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth
    ): AuthRepository = FirebaseAuthRepository(firebaseAuth)

    @Singleton
    val Context.colorDataStore by dataStore(
        fileName = "color_prefs.pb",
        serializer = ColorPreferencesSerializer
    )
}
