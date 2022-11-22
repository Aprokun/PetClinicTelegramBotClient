package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.*
import ru.mashurov.client.dtos.PetDto

interface PetClient {

    @POST("/api/pets/save")
    fun save(@Body pet: PetDto): Call<PetDto>

    @GET("/api/pets/{id}/get")
    fun get(@Path("id") id: Long): Call<PetDto>

    @DELETE("/api/pets/{id}/delete")
    fun delete(@Path("id") id: Long): Call<Void>
}