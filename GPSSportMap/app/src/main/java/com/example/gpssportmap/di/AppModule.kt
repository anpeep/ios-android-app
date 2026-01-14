package com.example.gpssportmap.di

import android.app.NotificationManager
import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.example.gpssportmap.coroutines.DefaultDispatcher
import com.example.gpssportmap.coroutines.NormalSharedPreferences
import com.example.gpssportmap.ui.compass.CompassSensor
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import jakarta.inject.Qualifier
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton


@Qualifier
@Retention(AnnotationRetention.RUNTIME)
annotation class ApplicationScope

@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Singleton
    @Provides
    fun provideNotificationManager(
        @ApplicationContext context: Context
    ): NotificationManager {
        return context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Provides
    @Singleton
    @NormalSharedPreferences
    fun provideSharedPreferences(@ApplicationContext context: Context): SharedPreferences {
        return context.getSharedPreferences("gps_sport_map_prefs", Context.MODE_PRIVATE)
    }

    @Provides
    @Singleton
    @ApplicationScope // Use the same qualifier from your injection site
    fun provideApplicationScope(
        // Hilt will get this dispatcher from your existing CoroutineModule.kt
        @DefaultDispatcher defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope {
        // Use a SupervisorJob so that the failure of one child coroutine doesn't cancel the whole scope
        return CoroutineScope(SupervisorJob() + defaultDispatcher)
    }

    @Singleton
    @Provides
    fun provideCompassSensor(@ApplicationContext context: Context): CompassSensor {

        return CompassSensor(context)
    }

    @Provides
    @Singleton
    fun provideEncryptedSharedPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        return EncryptedSharedPreferences.create(
            context,
            "auth_prefs",
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
        )
    }
}