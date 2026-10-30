package com.example.mikke.core.di

import com.example.mikke.feature.list.data.ListRepositoryImpl
import com.example.mikke.feature.list.domain.ListRepository
import com.example.mikke.feature.movie.data.MovieRepositoryImpl
import com.example.mikke.feature.movie.domain.MovieRepository
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
