package com.aura.domain.model

/**
 * Domain entity representing a user-selected or searched geographic location.
 */
data class LocationDomainModel(
    val name: String,
    val country: String,
    val admin1: String? = null,
    val latitude: Double,
    val longitude: Double
) {
    companion object {
        val DEFAULT = LocationDomainModel(
            name = "Madrid",
            country = "España",
            admin1 = "Madrid",
            latitude = 40.4168,
            longitude = -3.7038
        )
    }
}
