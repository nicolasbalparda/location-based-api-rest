package com.geermank.restaurants.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModel
import com.geermank.restaurants.repository.RestaurantsRepository
import com.geermank.restaurants.repository.models.AuthResponseWrapper

class MainViewModel(
    preferences: SharedPreferences
): ViewModel() {

    private val restaurantsRepository: RestaurantsRepository = RestaurantsRepository(preferences)

    private var latitude = 0.0
    private var longitude = 0.0

    fun renewToken(): LiveData<AuthResponseWrapper>{
        return restaurantsRepository.refreshToken()
    }

    fun storeLocation(latitude: Double, longitude: Double) {
        this.latitude = latitude
        this.longitude = longitude
    }


}