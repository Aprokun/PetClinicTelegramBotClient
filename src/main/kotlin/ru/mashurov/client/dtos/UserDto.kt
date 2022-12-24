package ru.mashurov.client.dtos

import lombok.Builder

@Builder
data class UserDto(
    val name: String,
    val username: String?,
    val telegramId: Long,
    var region: RegionDto?,
    val pets: MutableList<PetDto> = mutableListOf(),
    val id: Long? = null
)