package com.example.struku.di

import android.content.Context
import com.example.struku.presentation.auth.AuthManager
import com.example.struku.util.DataExporter
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Dependency injection module for application-level components
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    
    /**
     * Provides AuthManager as a singleton
     */
    @Provides
    @Singleton
    fun provideAuthManager(@ApplicationContext context: Context): AuthManager {
        return AuthManager(context)
    }
    
    /**
     * Provides DataExporter as a singleton
     */
    @Provides
    @Singleton
    fun provideDataExporter(@ApplicationContext context: Context): DataExporter {
        return DataExporter(context)
    }
}