package com.example.cineone.data.dto.membership

import kotlinx.serialization.Serializable

@Serializable
data class MyMembershipDto(
    val fullName: String = "",
    val email: String = "",
    val currentTier: String = "NORMAL",
    val totalSpent: Double = 0.0,
    val totalSpentDisplay: String = "0 đ",
    val nextTier: String? = null,
    val nextTierMinSpending: Double? = null,
    val nextTierMinSpendingDisplay: String? = null,
    val spendingToNextTier: Double? = null,
    val spendingToNextTierDisplay: String? = null,
    val progressPercentage: Double = 0.0,
    val currentTierBenefits: List<String> = emptyList(),
    val membershipCycle: String = "",
    val allTiers: List<TierDetailDto> = emptyList()
)

@Serializable
data class TierDetailDto(
    val tierId: Long = 0L,
    val name: String = "",
    val minSpending: Double = 0.0,
    val minSpendingDisplay: String = "",
    val description: String? = null,
    val benefits: List<String> = emptyList(),
    val isCurrent: Boolean = false,
    val colorHex: String = "#4CAF50"
)