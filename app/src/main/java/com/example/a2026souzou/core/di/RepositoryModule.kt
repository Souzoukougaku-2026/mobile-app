package com.example.a2026souzou.core.di

import com.example.a2026souzou.feature.list.data.ListRepositoryImpl
import com.example.a2026souzou.feature.list.domain.ListRepository
import com.example.a2026souzou.feature.movie.data.MovieRepositoryImpl
import com.example.a2026souzou.feature.movie.domain.MovieRepository
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
    abstract fun bindMovieRepository(
        movieRepositoryImpl: MovieRepositoryImpl
    ): MovieRepository

    @Binds
    @Singleton
    abstract fun bindListRepository(
        listRepositoryImpl: ListRepositoryImpl
    ): ListRepository
}
