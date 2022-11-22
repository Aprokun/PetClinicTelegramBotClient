package ru.mashurov.client.dtos

data class ClinicDto(
    val name: String,
    val address: String,
    val services: List<ServiceDto>,
    val id: Long? = null
)

fun ClinicDto.toNamedEntityDto() = NamedEntityDto(id!!, name)
