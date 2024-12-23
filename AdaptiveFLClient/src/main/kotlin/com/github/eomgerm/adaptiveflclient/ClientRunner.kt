package com.github.eomgerm.adaptiveflclient

import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.grpc.GrpcClient
import com.github.eomgerm.adaptiveflclient.rsocket.RSocketClient
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.springframework.stereotype.Component

private val logger = KotlinLogging.logger {}

@Component
class ClientRunner(
    private val grpcClient: GrpcClient,
    private val rSocketClient: RSocketClient,
    private val clientProperties: ClientProperties,
) {
    init {
        startClient()
    }

    private fun startClient() {
        var connected = false
        if (!clientProperties.reconnection.enabled) {
            connected = joinServer()
        } else {
            var attempts = clientProperties.reconnection.reconnectionAttempts ?: 1
            var delay = clientProperties.reconnection.reconnectionDelay

            while (attempts > 0) {
                connected = joinServer()
                if (connected) {
                    break
                }
                logger.warn { "Failed to connect to server. Retrying..." }
                if (clientProperties.reconnection.reconnectionAttempts != null) {
                    attempts--
                }
                delay =
                    if (delay * 2 > clientProperties.reconnection.reconnectionDelayMax) {
                        clientProperties.reconnection.reconnectionDelayMax
                    } else {
                        delay * 2
                    }
                runBlocking { delay(delay) }
            }
        }

        if (connected) {
            logger.info { "Successfully connected to server." }
        } else {
            logger.error { "Failed to connect to server." }
        }
    }

    private fun joinServer(): Boolean {
        try {
            grpcClient.joinToServer()
            rSocketClient.joinToServer()
        } catch (e: Exception) {
            return false
        }

        return true
    }
}
