package ru.mashurov.client.dtos

data class PetUpdateDto(
    var id: Long,
    var name: String,
    var gender: String,
    var age: Int
)