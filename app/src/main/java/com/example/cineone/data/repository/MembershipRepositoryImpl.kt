package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.dto.membership.MyMembershipDto
import com.example.cineone.data.remote.membership.MembershipApi
import com.example.cineone.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class MembershipRepositoryImpl(
    private val membershipApi: MembershipApi
) : MembershipRepository {

    override fun getMyMembership(): Flow<Resource<MyMembershipDto>> = flow {
        emit(Resource.Loading)
        try {
            val response = membershipApi.getMyMembership()
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data))
            } else {
                emit(Resource.Error(response.message ?: "Không tải được thông tin hạng thành viên"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}