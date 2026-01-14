package com.example.gpssportmap.coroutines // Or your equivalent package name

import javax.inject.Qualifier

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class NormalSharedPreferences

// Qualifier for the EncryptedSharedPreferences
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class EncryptedSharedPreferences

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher
