package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.*
import ru.mashurov.client.dtos.PetDto
import ru.mashurov.client.dtos.RegionDto
import ru.mashurov.client.dtos.UserDto

interface UserClient {

    @GET("/api/regions")
    fun getRegions(): Call<List<RegionDto>>

    @GET("/api/users/{id}/exists")
    fun existByTelegramId(@Path("id") id: Long, @Query("by") by: String): Call<Boolean>

    @POST("/api/users/save")
    fun save(@Body userDto: UserDto): Call<UserDto>

    @GET("/api/users/{id}/get")
    fun getUser(@Path("id") id: Long, @Query("by") by: String): Call<UserDto>

    @GET("/api/users/{id}/pets")
    fun getUserPets(@Path("id") id: Long): Call<List<PetDto>>
}