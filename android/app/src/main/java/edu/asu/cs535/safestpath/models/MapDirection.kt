package edu.asu.cs535.safestpath.models

import com.google.gson.annotations.SerializedName

data class MapDirection(var copyRights: String) {
    var summary: String = ""
    @SerializedName("overview_polyline") var polyLine: Polyline? = null
    var bounds: Bounds? = null
    var score: Float = 0.0f
}

data class Polyline(var points: String) {}
data class Bounds(var northeast: LatLngClass, var southwest: LatLngClass)
data class LatLngClass(var lat: Double, var lng: Double)