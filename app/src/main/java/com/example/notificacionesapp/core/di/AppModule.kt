package com.example.notificacionesapp.core.di

import android.content.Context
import com.example.notificacionesapp.SessionManager
import com.example.notificacionesapp.data.repository.AuthRepositoryImpl
import com.example.notificacionesapp.data.repository.NotificationRepositoryImpl
import com.example.notificacionesapp.data.repository.ScheduleRepositoryImpl
import com.example.notificacionesapp.domain.repository.AuthRepository
import com.example.notificacionesapp.domain.repository.NotificationRepository
import com.example.notificacionesapp.domain.repository.ScheduleRepository
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.createSupabaseClient
import io.github.jan.supabase.gotrue.Auth
import io.github.jan.supabase.postgrest.Postgrest
import io.github.jan.supabase.realtime.Realtime
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideSupabaseClient(): SupabaseClient {
        return createSupabaseClient(
            supabaseUrl = "https://dtsvtkynfnuccgamjlkx.supabase.co",
            supabaseKey = "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJpc3MiOiJzdXBhYmFzZSIsInJlZiI6ImR0c3Z0a3luZm51Y2NnYW1qbGt4Iiwicm9sZSI6ImFub24iLCJpYXQiOjE3Nzc2NzgzNzgsImV4cCI6MjA5MzI1NDM3OH0.3lAZYn6lRUCaM40TSK5Y5ZXlYYV5CMGAg3E5Fa9G44E"
        ) {
            install(Auth)
            install(Postgrest)
            install(Realtime)
        }
    }

    @Provides
    @Singleton
    fun provideGson(): Gson = GsonBuilder()
        .setDateFormat("yyyy-MM-dd HH:mm:ss")
        .create()

    @Provides
    @Singleton
    fun provideAuthRepository(
        supabaseClient: SupabaseClient,
        sessionManager: SessionManager
    ): AuthRepository = AuthRepositoryImpl(supabaseClient, sessionManager)

    @Provides
    @Singleton
    fun provideNotificationRepository(
        supabaseClient: SupabaseClient
    ): NotificationRepository = NotificationRepositoryImpl(supabaseClient)

    @Provides
    @Singleton
    fun provideScheduleRepository(
        @ApplicationContext context: Context,
        gson: Gson
    ): ScheduleRepository = ScheduleRepositoryImpl(context, gson)
}
