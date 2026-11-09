package com.example.cineone.domain.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.membership.MyMembershipDto
import kotlinx.coroutines.flow.Flow

interface MembershipRepository {
    fun getMyMembership(): Flow<Resource<MyMembershipDto>>
}