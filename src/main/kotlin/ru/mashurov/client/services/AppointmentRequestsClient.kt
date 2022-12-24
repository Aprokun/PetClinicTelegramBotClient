package ru.mashurov.client.services

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import ru.mashurov.client.dtos.AppointmentRequestDto
import ru.mashurov.client.dtos.PageResolver

interface AppointmentRequestsClient {

    @GET("/api/user/{id}/appointment-requests")
    fun findAllByUserId(@Path("id") id: Long): Call<PageResolver<AppointmentRequestDto>>

    @GET("/api/user/{userId}/appointment-request/{reqId}/cancel")
    fun cancelById(@Path("userId") userId: Long, @Path("reqId") reqId: Long): Call<Void>
}