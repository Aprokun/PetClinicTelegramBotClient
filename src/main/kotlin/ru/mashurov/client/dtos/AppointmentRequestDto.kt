package ru.mashurov.client.dtos

data class AppointmentRequestDto(
    var id: Long,
    var clinicName: String,
    var clinicAddress: String,
    var serviceName: String,
    var veterinarianName: String,
    var status: String,
    var appointmentPlace: String,
    var petName: String,
    var date: String
)