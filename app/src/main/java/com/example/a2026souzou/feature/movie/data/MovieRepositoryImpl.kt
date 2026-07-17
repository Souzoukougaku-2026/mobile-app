package com.example.a2026souzou.feature.movie.data

import com.example.a2026souzou.feature.movie.domain.MovieRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import kotlin.random.Random

class MovieRepositoryImpl @Inject constructor() : MovieRepository {
    override fun getBarValues(count: Int): Flow<Result<List<Float>>> = flow {
        try {
            val mockData = List(count) { Random.nextFloat() }
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }

    override fun getDetailedBarValues(count: Int): Flow<Result<List<Float>>> = flow {
        try {
            val mockData = List(count) { Random.nextFloat() }
            emit(Result.success(mockData))
        } catch (e: Exception) {
            emit(Result.failure(e))
        }
    }
}
