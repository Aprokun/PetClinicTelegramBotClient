package ru.mashurov.client.dtos

data class PetDto(
    val name: String,
    val age: Int,
    val gender: String,
    val appointments: List<AppointmentDto>,
    val id: Long? = null,
    val user: UserDto? = null
)

fun PetDto.toNamedEntityDto(): NamedEntityDto {
    return NamedEntityDto(id!!, name)
}