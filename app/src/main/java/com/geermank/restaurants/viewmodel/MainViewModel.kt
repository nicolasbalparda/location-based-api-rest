package com.geermank.restaurants.viewmodel

import android.content.SharedPreferences
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.geermank.restaurants.repository.RestaurantsRepository
import com.geermank.restaurants.repository.apimodels.AuthResponseWrapper
import com.geermank.restaurants.repository.apimodels.DataResponseWrapper
import com.geermank.restaurants.repository.models.Restaurant
import com.geermank.restaurants.utils.MoshiUtils
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
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

    private var data: MutableList<Restaurant?> = ArrayList()
    private var offset: Int = 0
    private var latitude: Double = 0.0
    private var longitude: Double = 0.0

    private val restaurantsRepository = RestaurantsRepository(preferences)

    private val executorService = Executors.newSingleThreadExecutor()

    private lateinit var adapter: JsonAdapter<List<Restaurant?>>

    fun renewToken(): LiveData<AuthResponseWrapper>{
        return restaurantsRepository.refreshToken()
    }

    fun storeLocation(latitude: Double?, longitude: Double?) {
        this.latitude = latitude ?: 0.0
        this.longitude = longitude ?: 0.0
    }

    /**
     * This method handle API calls to get restaurants. Notifies view once it gets
     * a response, whether it's successful or an error.
     *
     * If it is loading more items, this method also removes the loading item (a null value)
     * added to the list, in order to hide bottom ProgressBar in RecyclerView
     *
     * @param loadMoreItems determine whether the API request is to add more items
     * to the previously added, or it's the first request
     */
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

    fun getSerializedRestaurants(): String {
        initMoshiAdapter()

        return adapter.toJson(data)
    }

    fun restaurantsCount(): Int = data.size

    fun insertLoadingItemInRestaurantsList() = data.add(null)

    fun getLatitude(): Double = latitude

    fun getLongitude(): Double = longitude

    fun clearData(){
        data.clear()
        offset = 0
    }

    private fun removeLoadingItemInRestaurantsList() = data.remove(null)

    private fun initMoshiAdapter(){
        if (::adapter.isInitialized){
            return
        }
        adapter = MoshiUtils.restaurantsAdapter()
    }
}