package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.*
import ru.mashurov.client.dtos.AppointmentRequestDto
import ru.mashurov.client.dtos.ClinicDto
import ru.mashurov.client.dtos.VeterinarianDto

interface AppointmentRequestClient {

    @GET("/api/clinics")
    fun findAllClinics(@Query("region") region: Long): Call<MutableList<ClinicDto>>

    @GET("/api/clinics/{id}/get")
    fun findClinic(@Path("id") id: Long): Call<ClinicDto>

    @GET("/api/veterinarians")
    fun findAllVeterinarians(@Query("clinic") clinic: Long): Call<MutableList<VeterinarianDto>>

    @POST("/api/appointments/create")
    fun createRequest(@Body req: AppointmentRequestDto): Call<Void>

}