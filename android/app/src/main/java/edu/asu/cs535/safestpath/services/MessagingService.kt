package edu.asu.cs535.safestpath.services

import com.google.firebase.messaging.FirebaseMessagingService
import edu.asu.cs535.safestpath.utils.NetworkService

class MessagingService: FirebaseMessagingService() {
    override fun onNewToken(token: String) {
//        NetworkService().registerToken(token)
    }
}