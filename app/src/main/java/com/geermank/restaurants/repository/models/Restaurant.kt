package com.geermank.restaurants.repository.models

data class Restaurant(
    val name: String,
    val headerImage: String,
    val deliveryTime: String,
    val generalScore: Double,
    val shippingAmount: Double,
    val allCategories: String,
    val coordinates: String
)