package com.example.gpssportmap.data.network

import android.content.Context
import com.example.gpssportmap.ui.main.LockScreenSessionStore
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object SessionModule { // You can name this whatever you like

    @Singleton // Use if you want only one instance throughout the app
    @Provides
    fun provideLockScreenSessionStore(
        @ApplicationContext context: Context
    ): LockScreenSessionStore {
        return LockScreenSessionStore(context)
    }
}
