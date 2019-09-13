package com.geermank.restaurants.repository.models

import com.squareup.moshi.Json

data class Token(
    @field:Json(name = "access_token")
    val token: String

)