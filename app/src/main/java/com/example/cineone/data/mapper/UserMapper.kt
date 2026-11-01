package com.example.cineone.data.mapper

import com.example.cineone.data.dto.auth.UserDto
import com.example.cineone.domain.model.User

fun UserDto.toDomain(): User = User(
    userId = this.userId,
    email = this.email,
    fullName = this.fullName,
    phone = this.phone,
    dateOfBirth = this.dateOfBirth,
    address = this.address,
    membershipTier = this.membershipTier ?: "NORMAL",
    isVerified = this.isVerified ?: false,
    totalSpending = this.totalSpending ?: 0L
)