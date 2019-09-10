package com.geermank.restaurants.repository.models

/**
 * Data class for API response
 *
 * This class will hold data when making a request to the API, whether
 * the result is error or success
 *
 * @param exception holds error message to show to the user, if not translated
 * @param localizedMessage holds an id from String resources that represents localized error message
 * @param values list of values fetched from the service
 */
data class DataResponseWrapper<T>(
    var exception: String? = null,
    var localizedMessage: Int? = null,
    var values: List<T>? = null
)
