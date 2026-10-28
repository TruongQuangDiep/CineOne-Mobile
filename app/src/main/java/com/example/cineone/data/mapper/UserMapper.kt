package com.example.cineone.data.mapper

import com.example.cineone.data.dto.auth.UserDto
import com.example.cineone.domain.model.User

fun UserDto.toDomain(): User = User(
    userId = userId,
    email = email,
    fullName = fullName,
    phone = phone,
    membershipTier = membershipTier ?: "NORMAL",
    isVerified = isVerified ?: false
)