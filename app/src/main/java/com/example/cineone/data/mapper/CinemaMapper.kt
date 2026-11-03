package com.example.cineone.data.mapper

import com.example.cineone.data.dto.cinema.CinemaDto
import com.example.cineone.data.dto.food.FnbDto
import com.example.cineone.domain.model.Cinema
import com.example.cineone.domain.model.Fnb

fun CinemaDto.toDomain(): Cinema = Cinema(
    cinemaId = cinemaId,
    name = name,
    address = address,
    city = city,
    roomCount = roomCount,
    brandId = brandId,
    brandName = brandName.orEmpty(),
    brandLogoUrl = brandLogoUrl.orEmpty(),
    phone = phone.orEmpty(),
    latitude = latitude,
    longitude = longitude,
    status = status,
)

fun FnbDto.toDomain(): Fnb = Fnb(
    fnbId = fnbId,
    name = name,
    description = description.orEmpty(),
    price = price,
    imageUrl = imageUrl.orEmpty(),
    cinemaId = cinemaId,
    cinemaName = cinemaName.orEmpty(),
    status = status,
)

