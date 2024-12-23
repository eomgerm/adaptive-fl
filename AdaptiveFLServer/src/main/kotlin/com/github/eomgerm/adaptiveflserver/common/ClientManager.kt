package com.github.eomgerm.adaptiveflserver.common

import org.springframework.stereotype.Component

@Component
class ClientManager {
    private val clients = mutableMapOf<String, Client>()

    fun getAllClients(): List<Client> = clients.values.toList()

    fun addClient(client: Client) {
        clients[client.clientId] = client
    }

    fun removeClient(clientId: String) {
        clients.remove(clientId)
    }

    fun getClient(clientId: String): Client? = clients[clientId]

    fun clear() {
        clients.clear()
    }

    fun sample(n: Int): List<Client> = clients.values.shuffled().take(n)
}
