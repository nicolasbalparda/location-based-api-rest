package com.geermank.restaurants.utils

import com.geermank.restaurants.repository.models.Restaurant
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types

object MoshiUtils {

    fun restaurantsAdapter(): JsonAdapter<List<Restaurant?>> {

        val moshi = Moshi.Builder().build()

        val restaurantsListType = Types.newParameterizedType(List::class.java, Restaurant::class.java)
        return moshi.adapter(restaurantsListType)
    }

}