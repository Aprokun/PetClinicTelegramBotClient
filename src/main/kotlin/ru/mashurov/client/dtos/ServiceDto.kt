package ru.mashurov.client.dtos

data class ServiceDto(
    val name: String,
    val description: String,
    val cost: Int,
    val id: Long? = null
)

fun ServiceDto.toNamedEntityDto() = NamedEntityDto(id!!, name)