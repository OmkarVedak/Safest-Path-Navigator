package edu.asu.cs535.safestpath.utils

import com.google.gson.Gson
import edu.asu.cs535.safestpath.models.Crime
import edu.asu.cs535.safestpath.models.MapDirection
import okhttp3.*
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException


class NetworkService {
    private val client = OkHttpClient()

    // *********** Direction API **************** //
    interface DirectionListener {
        fun onComplete(directions: Array<MapDirection>)
        fun onFail()
    }

    private var directionListener: DirectionListener? = null

    fun getMapDirections(source: String, destination: String): NetworkService {
        val url = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.0.3")
            .port(5000)
            .addPathSegment("directions")
            .addQueryParameter("source", source)
            .addQueryParameter("destination", destination)
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                directionListener!!.onFail()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val directions =
                        Gson().fromJson(response.body!!.string(), Array<MapDirection>::class.java)
                    directionListener!!.onComplete(directions)
                }
            }
        })

        return this
    }

    fun onDirectionComplete(l: DirectionListener) {
        directionListener = l
    }

    // *********** Add Crime API **************** //
    interface TopicListener {
        fun onComplete()
        fun onFail()
    }

    private var topicListener: TopicListener? = null

    fun onTopicComplete(l: TopicListener) {
        topicListener = l
    }

    fun addTopic(latitude: Double, longitude: Double, parent_incident_type: String): NetworkService {
        val requestBody = "{ \"latitude\": \"$latitude\", \"longitude\": \"$longitude\", \"parent_incident_type\": \"$parent_incident_type\" }"
            .toRequestBody()

        val url = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.0.3")
            .port(5000)
            .addPathSegment("add_topic")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                if (topicListener !== null) {
                    topicListener!!.onFail()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    if (topicListener !== null) {
                        topicListener!!.onComplete()
                    }
                }
            }
        })

        return this
    }

    // *********** Nearby crime API **************** //
    interface NearbyListener {
        fun onComplete(crimes: Array<Crime>)
        fun onFail()
    }

    private var nearbyListener: NearbyListener? = null

    fun onNearbyComplete(l: NearbyListener) {
        nearbyListener = l
    }

    fun getNearby(latitude: Double, longitude: Double, radius: Double): NetworkService {
        val url = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.0.3")
            .port(5000)
            .addPathSegment("nearby")
            .addQueryParameter("lat", latitude.toString())
            .addQueryParameter("lng", longitude.toString())
            .addQueryParameter("radius", radius.toString())
            .build()

        val request = Request.Builder()
            .url(url)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                nearbyListener!!.onFail()
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    val crimes =
                        Gson().fromJson(response.body!!.string(), Array<Crime>::class.java)
                    nearbyListener!!.onComplete(crimes)
                }
            }
        })

        return this
    }

    // *********** Report unsafe path API **************** //
    interface UnsafeListener {
        fun onComplete()
        fun onFail()
    }

    private var unsafeListener: UnsafeListener? = null

    fun onNearbyComplete(l: UnsafeListener) {
        unsafeListener = l
    }

    fun reportNearby(path: String): NetworkService {
        val requestBody = "{ \"path\": \"$path\" }"
            .toRequestBody()

        val url = HttpUrl.Builder()
            .scheme("http")
            .host("192.168.0.3")
            .port(5000)
            .addPathSegment("report_unsafe")
            .build()

        val request = Request.Builder()
            .url(url)
            .post(requestBody)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                e.printStackTrace()
                if (unsafeListener !== null) {
                    unsafeListener!!.onFail()
                }
            }

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    if (!response.isSuccessful) throw IOException("Unexpected code $response")
                    if (unsafeListener !== null) {
                        unsafeListener!!.onComplete()
                    }
                }
            }
        })

        return this
    }
}