package edu.asu.cs535.safestpath.utils

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.location.Location
import android.os.Build
import android.util.Log
import com.google.android.gms.maps.model.BitmapDescriptor
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.DatabaseReference
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.messaging.FirebaseMessaging
import edu.asu.cs535.safestpath.services.LocationUpdatesService

fun createNotificationChannel(context: Context) {
    // Create the NotificationChannel, but only on API 26+ because
    // the NotificationChannel class is new and not in the support library
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val name = "General notification"
        val descriptionText = "Channel is for any safety notification"
        val importance = NotificationManager.IMPORTANCE_DEFAULT
        val channel = NotificationChannel("general_notification", name, importance).apply {
            description = descriptionText
        }
        // Register the channel with the system
        val notificationManager: NotificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }
}

fun subscribeToTopic(topic: String) {
    FirebaseMessaging.getInstance().subscribeToTopic(topic)
        .addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d("safestPath", "Not successful - subscribe to topic")
            } else {
                Log.d("safestPath", "Registered to topic")
            }
        }
}

fun unSubscribeTopic(topic: String) {
    FirebaseMessaging.getInstance().unsubscribeFromTopic(topic)
        .addOnCompleteListener { task ->
            if (!task.isSuccessful) {
                Log.d("safestPath", "Not successful - unsubscribe to topic")
            } else {
                Log.d("safestPath", "Unsubscribed to topic")
            }
        }
}

fun updateUserLocation(location: Location) {
    val user = FirebaseAuth.getInstance().currentUser
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference = database.getReference("users")

    if (user != null) {

        myRef.child(user.uid).child("lat").setValue(location.latitude)
        myRef.child(user.uid).child("long").setValue(location.longitude)
    }
}

fun updateUser(token: String) {
    val user = FirebaseAuth.getInstance().currentUser
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference  = database.getReference("users")

    if (user != null) {
        myRef.child(user.uid).child("token").setValue(token)
        myRef.child(user.uid).child("email").setValue(user.email)
        myRef.child(user.uid).child("fullName").setValue(user.displayName)
    }
}

fun updateUserPreferred(latitude: Double, longitude: Double) {
    val user = FirebaseAuth.getInstance().currentUser
    val database: FirebaseDatabase = FirebaseDatabase.getInstance()
    val myRef: DatabaseReference  = database.getReference("users")

    if (user != null) {
        myRef.child(user.uid).child("preferred").child("lat").setValue(latitude)
        myRef.child(user.uid).child("preferred").child("long").setValue(longitude)
    }
}

val crime_types = arrayOf(
    "Community Policing", "Theft", "Vehicle Stop", "Disorder", "Other", "Traffic", "Assault",
    "Proactive Policing", "Drugs", "Breaking & Entering", "Property Crime", "Pedestrian Stop", "Theft from Vehicle",
    "Theft of Vehicle", "Alarm", "Quality of Life", "Assault with Deadly Weapon", "Death", "Robbery", "Family Offense",
    "Other Sexual Offense", "Liquor", "Sexual Assault", "Weapons Offense", "Vehicle Recovery", "Missing Person", "Arson",
    "Sexual Offense", "Homicide", "Kidnapping"
)

fun getBitmapDescriptorFromVector(id: Int, context: Context): BitmapDescriptor {
    val vectorDrawable: Drawable? = context.getDrawable(id)
    val h = (32 * context.resources.displayMetrics.density).toInt();
    val w = (32 * context.resources.displayMetrics.density).toInt();
    vectorDrawable!!.setBounds(0, 0, w, h)
    val bm = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bm)
    vectorDrawable.draw(canvas)
    return BitmapDescriptorFactory.fromBitmap(bm)
}