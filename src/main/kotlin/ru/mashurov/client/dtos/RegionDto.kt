package ru.mashurov.client.dtos

data class RegionDto(
    val code: Long,
    val name: String
)

fun RegionDto.toNamedEntityDto() = NamedEntityDto(code, name)