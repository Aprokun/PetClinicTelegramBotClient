package ru.mashurov.client.dtos

data class PageResolver<T>(
    val content: List<T>,
    val totalPages: Int
)
