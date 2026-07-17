package com.example.a2026souzou.feature.movie.domain

import kotlinx.coroutines.flow.Flow

interface MovieRepository {
    fun getBarValues(count: Int): Flow<Result<List<Float>>>
    fun getDetailedBarValues(count: Int): Flow<Result<List<Float>>>
}
