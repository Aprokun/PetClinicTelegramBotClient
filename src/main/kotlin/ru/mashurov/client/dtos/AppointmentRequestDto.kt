package ru.mashurov.client.dtos

data class AppointmentRequestDto(
    val id: Long,
    val clinicName: String,
    val veterinarianName: String,
    val appointmentPlace: String,
    val petName: String,
    val serviceName: String
)