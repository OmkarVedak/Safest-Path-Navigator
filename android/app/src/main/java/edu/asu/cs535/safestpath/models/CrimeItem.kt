package edu.asu.cs535.safestpath.models

import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.clustering.ClusterItem

class CrimeItem(lat: Double, lng: Double, title: String = "", snippet: String = "") : ClusterItem {
    var mPosition: LatLng = LatLng(lat, lng)
    var mTitle: String = title
    var mSnippet: String = snippet

    override fun getPosition(): LatLng {
        return this.mPosition
    }

    override fun getTitle(): String {
        return this.mTitle
    }

    override fun getSnippet(): String {
        return this.mSnippet
    }
}