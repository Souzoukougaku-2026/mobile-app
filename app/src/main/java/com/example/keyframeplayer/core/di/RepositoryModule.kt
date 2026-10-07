package com.example.keyframeplayer.core.di

import com.example.keyframeplayer.feature.list.data.ListRepositoryImpl
import com.example.keyframeplayer.feature.list.domain.ListRepository
import com.example.keyframeplayer.feature.movie.data.MovieRepositoryImpl
import com.example.keyframeplayer.feature.movie.domain.MovieRepository
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
