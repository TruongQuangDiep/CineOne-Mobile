package com.example.cineone.data.dto.auth

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UserDto(
    @SerialName("userId")
    val userId: Long = 0L,

    @SerialName("email")
    val email: String = "",

    @SerialName("fullName")
    val fullName: String = "",

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerialName("homeAddress")
    val address: String? = null,

    @SerialName("membershipTier")
    val membershipTier: String? = "NORMAL",

    @SerialName("isVerified")
    val isVerified: Boolean? = false,

    @SerialName("emailVerified")
    val emailVerified: Boolean? = null,

    @SerialName("phoneVerified")
    val phoneVerified: Boolean? = null,

    @SerialName("totalSpending")
    val totalSpending: Long? = 0L,
)

@Serializable
data class UpdateProfileRequestDto(
    @SerialName("fullName")
    val fullName: String? = null,

    @SerialName("phone")
    val phone: String? = null,

    @SerialName("dateOfBirth")
    val dateOfBirth: String? = null,

    @SerialName("homeAddress")
    val homeAddress: String? = null,

    @SerialName("phoneVerified")
    val phoneVerified: Boolean? = null,
)
