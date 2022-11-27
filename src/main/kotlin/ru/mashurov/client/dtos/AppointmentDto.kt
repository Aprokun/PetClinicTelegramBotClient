package ru.mashurov.client.dtos

import java.util.*

data class AppointmentDto(
    val appointmentDate: Date,
    val appointmentPlace: String,
    val veterinarianDto: VeterinarianDto,
    val serviceDto: ServiceDto
)
