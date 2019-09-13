package com.geermank.restaurants.repository.apimodels

/**
 * Data class for API response
 *
 * This class will hold data to update UI, after making a request to the API, whether
 * the result is error or success
 *
 * @param data holds data obtained from service
 * @param exception holds error message to show to the user, if not translated
 * @param localizedMessage holds an id from String resources that represents localized error message
 */
data class DataResponseWrapper<T>(
    var data: List<T>? = null,
    var exception: String? = null,
    var localizedMessage: Int? = null
)
