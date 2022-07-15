package ru.netology.nmedia.modules

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.netology.nmedia.api.*
import ru.netology.nmedia.auth.AppAuth
import javax.inject.Singleton

@InstallIn(SingletonComponent::class)
@Module
object ApiServiceModule {
    @Provides
    @Singleton
    fun provideApiService(auth: AppAuth): ApiService {
        return retrofit(okhttp(loggingInterceptor(), authInterceptor(auth)))
            .create(ApiService::class.java)
    }
}