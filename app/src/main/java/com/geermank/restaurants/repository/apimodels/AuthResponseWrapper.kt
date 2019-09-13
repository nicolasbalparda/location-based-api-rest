package com.geermank.restaurants.repository.apimodels

data class AuthResponseWrapper(
    var exception: String? = null,
    var localizedMessage: Int? = null
)