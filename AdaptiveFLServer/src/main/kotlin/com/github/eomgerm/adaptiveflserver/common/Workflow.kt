package com.github.eomgerm.adaptiveflserver.common

import com.github.eomgerm.adaptiveflserver.*
import com.github.eomgerm.adaptiveflserver.config.ServerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import kotlinx.coroutines.*
import org.springframework.shell.standard.ShellComponent
import org.springframework.shell.standard.ShellMethod

private val logger = KotlinLogging.logger {}

@ShellComponent
class Workflow(
    private val clientManager: ClientManager,
    private val serverProperties: ServerProperties,
    private val messageQueue: ClientMessageQueue,
) {
    @ShellMethod(key = ["start"], value = "Start the workflow")
    fun start() {
        if (clientManager.getAllClients().isEmpty()) {
            logger.warn { "No clients are connected. Please connect clients first." }
            return
        }
        logger.info { "Connected Clients: ${clientManager.getAllClients().map { it.clientId }}" }
        logger.info { "[INIT]" }
        initParameters()
        logger.info { "[FIT]" }
        fit()
    }

    @ShellMethod(key = ["test"], value = "Test the workflow")
    fun test() {
        val requester =
            clientManager.getAllClients().first().rSocketRequester
                ?: throw IllegalArgumentException("No RSocketRequester provided.")

        val res =
            requester
                .route("hello")
                .data("Server")
                .retrieveMono(String::class.java)
                .block()

        logger.info { res }
    }

    private fun initParameters() {
        logger.info { "Requesting initial parameters from one of the clients" }
        val randomClient = clientManager.sample(1).first()
        val client = AdaptiveClient(randomClient, serverProperties, messageQueue)
        client.getParameters()

        val message = messageQueue.get(randomClient, ClientParametersResult::class.java)

        val initialParameters = message.data.parameters
        if (initialParameters.isEmpty()) {
            logger.warn { "Failed to get initial parameters from client. Using random parameters." }
        } else {
            logger.info { "Received initial parameters: ${initialParameters.take(10)}..." }
        }
    }

    private fun fit() {
        repeat(3) { round ->
            logger.info { "[ROUND ${round + 1}]" }
            val commClients = clientManager.getAllClients().map { AdaptiveClient(it, serverProperties, messageQueue) }

            fitRound(commClients)
            evaluateRound(commClients)
        }
    }

    private fun fitRound(commClients: List<AdaptiveClient>) {
        val results = mutableMapOf<Client, ClientFitResult>()
        runBlocking {
            commClients
                .map { client ->
                    async(Dispatchers.IO) {
                        client.fit(Model.getParameters())
                    }
                }.awaitAll()
        }

        runBlocking {
            commClients
                .map { commClient ->
                    async(Dispatchers.IO) {
                        val message = messageQueue.get(commClient.client, ClientFitResult::class.java)
                        results[commClient.client] = message.data
                    }
                }.awaitAll()
        }

        logger.info { "aggregateFit" }
        results.entries.forEach { (client, result) ->
            logger.info { "Client: ${client.clientId}, Status: ${result.status}, Parameters: ${result.parameters.take(10)}..." }
        }
    }

    private fun evaluateRound(commClients: List<AdaptiveClient>) {
        val results = mutableMapOf<Client, ClientEvaluateResult>()
        runBlocking {
            commClients
                .map { commClient ->
                    async(Dispatchers.IO) {
                        commClient.evaluate(Model.getParameters())
                    }
                }.awaitAll()
        }

        runBlocking {
            commClients
                .map { commClient ->
                    async(Dispatchers.IO) {
                        val message = messageQueue.get(commClient.client, ClientEvaluateResult::class.java)
                        results[commClient.client] = message.data
                    }
                }.awaitAll()
        }

        logger.info { "aggregateEvaluate" }
        results.entries.forEach { (client, result) ->
            logger.info { "Client: ${client.clientId}, Loss: ${result.loss} Num of samples: ${result.numSamples}" }
        }
    }
}
