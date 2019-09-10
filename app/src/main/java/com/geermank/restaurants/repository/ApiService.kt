package com.geermank.restaurants.repository

import com.geermank.restaurants.repository.models.Token

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query

interface ApiService {

    @GET("tokens")
    fun getToken(@Query("clientId") clientId: String,
                 @Query("clientSecret") clientSecret: String): Call<Token>

}