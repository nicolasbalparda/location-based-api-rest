package com.geermank.restaurants.repository

import android.content.SharedPreferences
import android.util.Log
import androidx.annotation.WorkerThread
import com.geermank.restaurants.R
import com.geermank.restaurants.repository.apimodels.DataResponseWrapper
import com.geermank.restaurants.repository.models.Restaurant
import java.io.IOException

class RestaurantsRepository(
    preferences: SharedPreferences
):BaseRepository(preferences) {

    @Throws(IOException::class)
    @WorkerThread
    fun getRestaurants(latitude: Double, longitude: Double, offset: Int): DataResponseWrapper<Restaurant> {

        val params = buildParamsForMainActivity(latitude,longitude,offset)

        val response = getApiService().getNearbyRestaurants(params).execute()
        if (response.isSuccessful){
            return DataResponseWrapper(response.body()?.data)
        }

        //not successful response
        return DataResponseWrapper(exception = response.message(),
            localizedMessage = getLocalizedResponseMessage(response.code()))

    }

    private fun buildParamsForMainActivity(latitude: Double, longitude: Double, offset: Int): HashMap<String,Any>{
        val point = "$latitude,$longitude"
        val country = 3 //TODO Remove hardcoded country
        val max = 20
        val fields = "name,headerImage,deliveryTime,generalScore,shippingAmount,allCategories,coordinates"

        return buildParams(point,country,max,fields,offset)
    }

    private fun buildParams(point: String, country: Int, max: Int, fields: String, offset: Int): HashMap<String,Any> {

        val params = HashMap<String,Any>()
        params["point"] = point
        params["country"] = country
        params["fields"] = fields
        params["offset"] = offset
        params["max"] = max

        return params
    }

}