package com.ireddragonicy.llmdroid.di

import android.content.Context
import com.ireddragonicy.llmdroid.data.local.SecureStorage
import com.ireddragonicy.llmdroid.data.remote.auth.AuthConfig
import com.ireddragonicy.llmdroid.data.remote.downloader.ModelDownloader
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import net.openid.appauth.AuthorizationService
import okhttp3.OkHttpClient
import javax.inject.Qualifier
import javax.inject.Singleton

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun provideAuthorizationService(@ApplicationContext context: Context): AuthorizationService {
        return AuthorizationService(context)
    }

    @Provides
    @Singleton
    fun provideSecureStorage(@ApplicationContext context: Context): SecureStorage {
        return SecureStorage(context)
    }

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder().build()
    }

    @Provides
    @Singleton
    fun provideModelDownloader(
        okHttpClient: OkHttpClient,
        secureStorage: SecureStorage,
        @ApplicationContext context: Context,
        @IoDispatcher ioDispatcher: CoroutineDispatcher
    ): ModelDownloader {
        return ModelDownloader(okHttpClient, secureStorage, context, ioDispatcher)
    }

    @Provides
    fun provideAuthConfig(): AuthConfig {
        return AuthConfig
    }

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

}