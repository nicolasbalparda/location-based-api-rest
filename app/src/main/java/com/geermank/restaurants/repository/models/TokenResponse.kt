package com.geermank.restaurants.repository.models

/**
 * Data class for token response
 *
 * This class will hold data when making a request to the API to renew user token, whether
 * the result is error or success
 *
 * @param message will contain text result to show to the user, if not translated
 * @param code indicates Http result code. By default is initialized in -1, if error does not relies on Http result
 * @param error flag that quickly indicates what kind of result we got from request
 */
data class TokenResponse(
    val message: String,
    val code: Int = -1,
    var error: Boolean = true
)
