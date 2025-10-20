package com.example.notificacionesapp.core.di

import android.content.Context
import com.example.notificacionesapp.data.repository.AuthRepositoryImpl
import com.example.notificacionesapp.data.repository.NotificationRepositoryImpl
import com.example.notificacionesapp.data.repository.ScheduleRepositoryImpl
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.domain.repository.ScheduleRepository
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module providing application-level dependencies
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideFirebaseAuth(): FirebaseAuth = FirebaseAuth.getInstance()

    @Provides
    @Singleton
    fun provideFirebaseDatabase(): FirebaseDatabase = FirebaseDatabase.getInstance()

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    @Provides
    @Singleton
    fun provideAuthRepository(
        firebaseAuth: FirebaseAuth,
        firebaseDatabase: FirebaseDatabase
    ): AuthRepository = AuthRepositoryImpl(firebaseAuth, firebaseDatabase)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): NotificationRepository = NotificationRepositoryImpl(context, gson)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): ScheduleRepository = ScheduleRepositoryImpl(context, gson)
}
