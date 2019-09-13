package com.geermank.restaurants.repository.apimodels

data class DataResponse<T>(
    val total: Int,
    val max: Int,
    val sort: String,
    val count: Int,
    val data: List<T>
)