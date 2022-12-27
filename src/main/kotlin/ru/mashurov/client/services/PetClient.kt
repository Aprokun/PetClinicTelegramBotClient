package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.*
import ru.mashurov.client.dtos.AppointmentRequestDto
import ru.mashurov.client.dtos.PetDto
import ru.mashurov.client.dtos.PetUpdateDto

interface PetClient {

    @POST("/api/pets/save")
    fun save(@Body pet: PetDto): Call<PetDto>

    @GET("/api/pets/{id}/get")
    fun get(@Path("id") id: Long): Call<PetDto>

    @DELETE("/api/pets/{id}/delete")
    fun delete(@Path("id") id: Long): Call<Void>

    @PUT("/api/pets/update")
    fun update(@Body petUpdateDto: PetUpdateDto): Call<Void>

    @GET("/api/appointments/{petId}/history")
    fun getAppointmentHistory(@Path("petId") id: Long): Call<List<AppointmentRequestDto>>
}