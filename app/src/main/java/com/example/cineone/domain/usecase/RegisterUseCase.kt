package com.example.cineone.domain.usecase

import com.example.cineone.core.common.Resource
import com.example.cineone.domain.model.User
import com.example.cineone.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow

class RegisterUseCase(private val repository: AuthRepository) {
    operator fun invoke(email: String, pass: String, name: String, phone: String?): Flow<Resource<User>> {
        return repository.register(email.trim(), pass, name.trim(), phone?.trim())
    }
}