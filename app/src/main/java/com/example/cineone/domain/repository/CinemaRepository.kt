package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Fnb
import kotlinx.coroutines.flow.Flow

interface CinemaRepository {
    fun getCinemas(
        q: String? = null,
        city: String? = null,
        status: String? = null,
        page: Int = 0
    ): Flow<Resource<List<Cinema>>>

    fun getCinemaDetail(cinemaId: Long): Flow<Resource<Cinema>>

    fun getCinemaFnbs(cinemaId: Long): Flow<Resource<List<Fnb>>>
}

