package edu.asu.cs535.safestpath.models

import com.google.gson.annotations.SerializedName

data class Crime(val latitude: Double = 0.0) {
    val longitude: Double = 0.0
    @SerializedName("parent_incident_type") var type: String = ""
    @SerializedName("incident_datetime") var time: String = ""
}