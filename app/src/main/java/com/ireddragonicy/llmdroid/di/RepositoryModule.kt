package com.ireddragonicy.llmdroid.di

import com.ireddragonicy.llmdroid.data.repository.AuthRepositoryImpl
import com.ireddragonicy.llmdroid.data.repository.ChatRepositoryImpl
import com.ireddragonicy.llmdroid.data.repository.ModelRepositoryImpl
import com.ireddragonicy.llmdroid.domain.repository.AuthRepository
import com.ireddragonicy.llmdroid.domain.repository.ChatRepository
import com.ireddragonicy.llmdroid.domain.repository.ModelRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton


@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindChatRepository(
        chatRepositoryImpl: ChatRepositoryImpl
    ): ChatRepository

    @Binds
    @Singleton
    abstract fun bindModelRepository(
        modelRepositoryImpl: ModelRepositoryImpl
    ): ModelRepository

    @Binds
    @Singleton
    abstract fun bindAuthRepository(
        authRepositoryImpl: AuthRepositoryImpl
    ): AuthRepository
}