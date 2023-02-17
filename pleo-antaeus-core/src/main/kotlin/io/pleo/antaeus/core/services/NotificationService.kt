package io.pleo.antaeus.core.services

import mu.KotlinLogging

class NotificationService {
    private val logger = KotlinLogging.logger {}

    fun sendNotification(message: String) {
        logger.info("Message for customer: $message")
    }
}
