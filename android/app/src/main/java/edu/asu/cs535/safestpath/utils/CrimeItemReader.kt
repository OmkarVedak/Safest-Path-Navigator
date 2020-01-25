package edu.asu.cs535.safestpath.utils

import edu.asu.cs535.safestpath.models.CrimeItem
import java.io.InputStream
import org.json.JSONArray
import java.util.*
import kotlin.collections.ArrayList


class CrimeItemReader {
    companion object {
        private const val REGEX_INPUT_BOUNDARY_BEGINNING: String = "\\A"
    }

    fun read(inputStream: InputStream): List<CrimeItem> {
        val items = ArrayList<CrimeItem>()
        val json = Scanner(inputStream).useDelimiter(REGEX_INPUT_BOUNDARY_BEGINNING).next()
        val array = JSONArray(json)
        for (i in 0 until array.length()) {
            val obj = array.getJSONObject(i)
            var title: String = ""
            var snippet: String = ""
            val lat = obj.getDouble("latitude")
            val lng = obj.getDouble("longitude")
            if (!obj.isNull("parent_incident_type")) {
                title = obj.getString("parent_incident_type")
            }
            if (!obj.isNull("snippet")) {
                snippet = obj.getString("snippet")
            }
            items.add(CrimeItem(lat, lng, title, snippet))
        }
        return items
    }
}