package com.geermank.restaurants.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.geermank.restaurants.repository.RestaurantsRepository
import com.geermank.restaurants.repository.apimodels.AuthResponseWrapper
import com.geermank.restaurants.repository.apimodels.DataResponseWrapper
import com.geermank.restaurants.repository.models.Restaurant
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainViewModel(
    preferences: SharedPreferences
): ViewModel() {

    var restaurants = MutableLiveData<List<Restaurant?>>()
    var error = MutableLiveData<String>()
    var localizedError = MutableLiveData<Int>()

    var loadingMoreItems: Boolean = false

    private val executorService = Executors.newSingleThreadExecutor()

    private val restaurantsRepository = RestaurantsRepository(preferences)

    private var data: MutableList<Restaurant?> = ArrayList()
    private var offset: Int = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    fun renewToken(): LiveData<AuthResponseWrapper>{
        return restaurantsRepository.refreshToken()
    }

    fun storeLocation(latitude: Double?, longitude: Double?) {
        this.latitude = latitude ?: 0.0
        this.longitude = longitude ?: 0.0
    }

    fun getNearbyRestaurants(loadMoreItems: Boolean) {

        loadingMoreItems = loadMoreItems

        executorService.submit {
            try{
                val response = restaurantsRepository.getRestaurants(latitude, longitude, offset)

                if (response.exception != null){
                    // error fetching data from service
                    localizedError.postValue(response.localizedMessage)
                    return@submit
                }

                if (loadingMoreItems){
                    removeLoadingItemInRestaurantsList()
                    loadingMoreItems = false
                }

                // got data from service successfully
                data.addAll(response.data!!)
                restaurants.postValue(data)

                offset = data.size

            }catch (ex: IOException){
                error.postValue(ex.localizedMessage)
            }
        }
    }

    fun restaurantsCount(): Int = data.size

    fun insertLoadingItemInRestaurantsList() = data.add(null)

    private fun removeLoadingItemInRestaurantsList() = data.remove(null)

}