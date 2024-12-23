package com.github.eomgerm.adaptiveflserver.rsocket

import com.github.eomgerm.adaptiveflserver.ClientEvaluateResult
import com.github.eomgerm.adaptiveflserver.ClientFitResult
import com.github.eomgerm.adaptiveflserver.ClientParametersResult
import com.github.eomgerm.adaptiveflserver.MessageStatus
import com.github.eomgerm.adaptiveflserver.common.Client
import com.github.eomgerm.adaptiveflserver.common.ClientManager
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.messaging.handler.annotation.Payload
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.messaging.rsocket.RSocketStrategies
import org.springframework.stereotype.Controller
import org.springframework.util.MimeTypeUtils
import reactor.core.publisher.Flux

private val logger = KotlinLogging.logger {}

@Controller
class RSocketController(
    private val clientManager: ClientManager,
    private val messageQueue: ClientMessageQueue,
    private val rSocketStrategies: RSocketStrategies,
) {
    @MessageMapping("join")
    fun join(
        @Payload req: JoinIns,
    ) {
        logger.info { "Client joined: ${req.clientId}" }

        val rSocketRequester =
            RSocketRequester
                .builder()
                .rsocketConnector {
                    it.fragment(16_777_215)
                }.dataMimeType(MimeTypeUtils.APPLICATION_JSON)
                .rsocketStrategies(rSocketStrategies)
                .tcp(req.host, req.port)

        clientManager.getClient(req.clientId)?.rSocketRequester = rSocketRequester
//        rSocketRequester
//            .rsocket()!!
//            .onClose()
//            .doFinally {
//                clientManager.getClient(req.clientId)?.rSocketRequester = null
//            }.subscribe()
    }

    @MessageMapping("initParams")
    fun initParams(
        @Payload req: Flux<GetParametersRes>,
    ) {
        var client: Client? = null
        var status = RSocketStatus.NOOP
        val parameters = mutableListOf<Double>()
        req
            .doOnEach {
                it.get()?.let { data ->
                    client = clientManager.getClient(data.clientId)
                    status = data.status
                    parameters.addAll(data.parameters)
                }
            }.blockLast()

        if (client == null) {
            logger.warn { "Client not found" }
            return
        }

        if (status == RSocketStatus.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        val result =
            ClientParametersResult(
                status = MessageStatus.fromRSocket(status),
                parameters = parameters,
            )

        messageQueue.add(ClientMessageQueue.Message(client!!, result))
    }

    @MessageMapping("fit")
    fun fit(
        @Payload req: Flux<FitRes>,
    ) {
        var client: Client? = null
        var numSamples = 0L
        var status = RSocketStatus.NOOP
        val parameters = mutableListOf<Double>()
        req
            .doOnEach {
                it.get()?.let { data ->
                    client = clientManager.getClient(data.clientId)
                    status = data.status
                    numSamples = data.numSamples
                    parameters.addAll(data.parameters)
                }
            }.blockLast()

        if (client == null) {
            logger.warn { "Client not found" }
            return
        }

        if (status == RSocketStatus.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        val result =
            ClientFitResult(
                status = MessageStatus.fromRSocket(status),
                parameters = parameters,
                numSamples = numSamples,
            )
        messageQueue.add(ClientMessageQueue.Message(client!!, result))
    }

    @MessageMapping("evaluate")
    fun evaluate(
        @Payload req: EvaluateRes,
    ) {
        val client = clientManager.getClient(req.clientId) ?: return

        if (req.status == RSocketStatus.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        val result =
            ClientEvaluateResult(
                status = MessageStatus.fromRSocket(req.status),
                loss = req.loss,
                numSamples = req.numSamples,
            )
        messageQueue.add(ClientMessageQueue.Message(client, result))
    }
}
