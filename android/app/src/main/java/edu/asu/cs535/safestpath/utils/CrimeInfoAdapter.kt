package edu.asu.cs535.safestpath.utils

import android.app.Activity
import android.content.Context
import android.view.View
import android.widget.TextView
import com.google.android.gms.maps.GoogleMap
import com.google.android.gms.maps.model.Marker
import edu.asu.cs535.safestpath.R
import edu.asu.cs535.safestpath.models.Crime

class CrimeInfoAdapter(ctx: Context) : GoogleMap.InfoWindowAdapter {

    private var context: Context = ctx

    override fun getInfoContents(marker: Marker?): View {
        val view: View = (context as Activity).layoutInflater.inflate(R.layout.crime_info_window, null)

        val title: TextView = view.findViewById(R.id.crimeTitle)
        val description: TextView = view.findViewById(R.id.crimeDetails)

        val crime: Crime = marker!!.tag as Crime

        title.text = crime.type
        description.text = "Incident Time: ${crime.time}"

        return view
    }

    override fun getInfoWindow(marker: Marker?): View? {
        return null
    }
}