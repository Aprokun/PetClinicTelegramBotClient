package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.GET
import ru.mashurov.client.dtos.RegionDto

interface RegionClient {

    @GET("/api/regions")
    fun findAll(): Call<List<RegionDto>>
}