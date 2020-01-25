package edu.asu.cs535.safestpath.utils

import android.content.Context
import android.view.View
import android.widget.RelativeLayout
import com.google.android.gms.maps.model.*
import edu.asu.cs535.safestpath.R
import edu.asu.cs535.safestpath.models.Bounds

fun setLocationButton(mapView: View) {
    val locationButton =
        (mapView.findViewById<View>(Integer.parseInt("1")).parent as View).findViewById<View>(
            Integer.parseInt("2")
        )
    val layoutParams = locationButton.layoutParams as (RelativeLayout.LayoutParams)
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_TOP, 0)
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM, RelativeLayout.TRUE)
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_START, RelativeLayout.TRUE)
    layoutParams.addRule(RelativeLayout.ALIGN_PARENT_END, 0)
    layoutParams.setMargins(80, 0, 0, 100);
    layoutParams.width = 150
    layoutParams.height = 150
}

fun getLatLngString(latLng: LatLng): String {
    return "${latLng.latitude},${latLng.longitude}"
}

fun getLatLngBound(bound: Bounds): LatLngBounds {
    val southwest = LatLng(bound.southwest.lat, bound.southwest.lng)
    val northeast = LatLng(bound.northeast.lat, bound.northeast.lng)
    return LatLngBounds(southwest, northeast)
}

fun getPolylineOptions(coordinates: List<LatLng>, context: Context): PolylineOptions {
    return PolylineOptions()
        .addAll(coordinates)
        .clickable(true)
        .color(context.getColor(R.color.routeBackground))
        .width(25f)
        .startCap(RoundCap())
        .endCap(RoundCap())
        .jointType(JointType.ROUND)
        .zIndex(1000f)
        .geodesic(true)
}
