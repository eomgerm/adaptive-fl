package com.github.eomgerm.adaptiveflserver.common

import org.springframework.stereotype.Component
import java.util.LinkedList
import kotlin.time.Duration.Companion.minutes

@Component
class ClientMessageQueue {
    private val queue = LinkedList<Message<*>>()
    private val timeout = 1.minutes

    fun add(message: Message<*>) {
        queue.add(message)
    }

    fun <MessageType> get(
        client: Client,
        messageType: Class<MessageType>,
    ): Message<MessageType> {
        while (true) {
            val message = queue.peek() ?: continue
            if (message.client.clientId == client.clientId && messageType.isInstance(message.data)) {
                return queue.poll() as Message<MessageType>
            }
        }
    }

    data class Message<T>(
        val client: Client,
        val data: T,
    )
}
