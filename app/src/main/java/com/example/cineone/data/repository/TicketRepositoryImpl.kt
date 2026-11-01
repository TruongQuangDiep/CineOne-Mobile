package com.example.cineone.data.repository

import com.example.cineone.core.common.Resource
import com.example.cineone.data.mapper.toDomain
import com.example.cineone.data.remote.ticket.TicketApi
import com.example.cineone.domain.model.Ticket
import com.example.cineone.domain.model.TicketDetail
import com.example.cineone.domain.repository.TicketRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class TicketRepositoryImpl(private val ticketApi: TicketApi) : TicketRepository {

    override fun getMyTickets(tab: String): Flow<Resource<List<Ticket>>> = flow {
        emit(Resource.Loading)
        try {
            val response = ticketApi.getMyTickets(tab)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.map { it.toDomain() }))
            } else {
                emit(Resource.Error(response.message ?: "Không lấy được danh sách vé"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }

    override fun getTicketDetail(ticketId: Long): Flow<Resource<TicketDetail>> = flow {
        emit(Resource.Loading)
        try {
            val response = ticketApi.getTicketDetail(ticketId)
            if (response.success && response.data != null) {
                emit(Resource.Success(response.data.toDomain()))
            } else {
                emit(Resource.Error(response.message ?: "Không tìm thấy chi tiết vé"))
            }
        } catch (e: Exception) {
            emit(Resource.Error(e.localizedMessage ?: "Lỗi kết nối"))
        }
    }
}