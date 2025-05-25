package com.aus20.service

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.Message
import org.springframework.stereotype.Service


@Service
class NotificationService {

    fun sendNotification(token: String, title: String, body: String) {
        val message = Message.builder()
            .setToken(token)
            .putData("title", title)
            .putData("body", body)
            .build()

        FirebaseMessaging.getInstance().sendAsync(message)
    }
}