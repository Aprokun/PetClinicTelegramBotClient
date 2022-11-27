package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import ru.mashurov.client.dtos.AppointmentDto

interface AppointmentClient {

    @GET("/api/appointments/{petId}/history")
    fun fetchHistory(@Path("petId") petId: Long): Call<List<AppointmentDto>>
}