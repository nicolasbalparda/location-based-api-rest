package com.geermank.restaurants.repository

import com.geermank.restaurants.repository.apimodels.DataResponse
import com.geermank.restaurants.repository.models.Restaurant
import com.geermank.restaurants.repository.models.Token

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Query
import retrofit2.http.QueryMap

interface ApiService {

    @GET("tokens")
    fun getToken(@Query("clientId") clientId: String,
                 @Query("clientSecret") clientSecret: String): Call<Token>

    @GET("search/restaurants")
    fun getNearbyRestaurants(@QueryMap params: HashMap<String,Any>): Call<DataResponse<Restaurant>>

}