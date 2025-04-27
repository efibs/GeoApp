package de.fibs.geoappandroid.models

import kotlinx.serialization.Serializable

@Serializable
data class Datapoint (
    val latitude: Double,
    val longitude: Double,
    val steps: Int,
    val timestamp: String
)