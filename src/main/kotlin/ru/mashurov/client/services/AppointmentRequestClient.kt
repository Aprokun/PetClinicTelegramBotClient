package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.*
import ru.mashurov.client.dtos.*

interface AppointmentRequestClient {

    @GET("/api/clinics")
    fun findAllClinics(@Query("region") region: Long): Call<PageResolver<ClinicDto>>

    @GET("/api/clinics/{id}")
    fun findClinic(@Path("id") id: Long): Call<ClinicDto>

    @GET("/api/clinics/{id}/veterinarians")
    fun findAllVeterinariansByClinicId(@Path("id") clinicId: Long): Call<PageResolver<VeterinarianDto>>

    @POST("/api/appointments/create")
    fun createRequest(@Body req: AppointmentRequestCreateDto): Call<Void>

    @GET("/api/services/{id}")
    fun findService(@Path("id") id: Long): Call<ServiceDto>

    @GET("/api/veterinarians/{id}")
    fun findVeterinarian(@Path("id") id: Long): Call<VeterinarianDto>

    @GET("/api/veterinarians/{id}/allow-timetable")
    fun findAllowTimePeriodsByVeterinarianIdAndDate(
        @Path("id") id: Long, @Query("date") date: String
    ): Call<List<TimePeriod>>

}