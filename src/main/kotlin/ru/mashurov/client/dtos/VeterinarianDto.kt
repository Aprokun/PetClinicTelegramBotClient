package ru.mashurov.client.dtos

data class VeterinarianDto(
    val name: String = "",
    val surname: String = "",
    val patronymic: String? = null,
    val experience: Int = 0,
    val id: Long? = null
)

fun VeterinarianDto.getSNP() = "$surname $name $patronymic"

fun VeterinarianDto.toNamedEntityDto() = NamedEntityDto(id!!, name)
