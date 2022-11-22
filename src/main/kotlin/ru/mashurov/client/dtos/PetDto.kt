package ru.mashurov.client.dtos

data class PetDto(
    val name: String,
    val age: Int,
    val gender: String,
    val id: Long? = null,
    val user: UserDto? = null
)