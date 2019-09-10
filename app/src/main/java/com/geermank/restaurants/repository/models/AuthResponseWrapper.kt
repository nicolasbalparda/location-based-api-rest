package com.geermank.restaurants.repository.models

data class AuthResponseWrapper(
    var exception: String? = null,
    var localizedMessage: Int? = null
)