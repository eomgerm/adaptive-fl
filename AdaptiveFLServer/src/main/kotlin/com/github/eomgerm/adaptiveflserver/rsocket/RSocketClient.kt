package com.github.eomgerm.adaptiveflserver.rsocket

import com.github.eomgerm.adaptiveflserver.*
import com.github.eomgerm.adaptiveflserver.common.Client
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.rsocket.RSocketRequester
import reactor.core.publisher.Flux
import kotlin.time.Duration
import kotlin.time.toJavaDuration

private val logger = KotlinLogging.logger {}

class RSocketClient(
    private val client: Client,
    private val messageQueue: ClientMessageQueue,
    private val rSocketRequester: RSocketRequester =
        client.rSocketRequester
            ?: throw IllegalArgumentException("No RSocketRequester provided."),
) : CommClient {
    override fun getParameters() {
        var status = RSocketStatus.NOOP
        val parameters = mutableListOf<Double>()

        rSocketRequester
            .route("initParams")
            .retrieveFlux(GetParametersRes::class.java)
            .doOnEach {
                it.get()?.let { res ->
                    status = res.status
                    if (status == RSocketStatus.NOOP) {
                        return@let
                    }
                    parameters.addAll(res.parameters)
                }
            }.blockLast()

        if (status == RSocketStatus.NOOP) {
            logger.info { "[RSocket]: Client sent data by other transport" }
            return
        }

        val messageStatus = MessageStatus.fromRSocket(status)
        messageQueue.add(
            Message(
                client,
                ClientParametersResult(messageStatus, parameters),
            ),
        )
    }

    override fun fit(
        parameters: List<Double>,
        chunkSize: Int,
        timeout: Duration,
    ) {
        var status = RSocketStatus.NOOP
        val fitParameters = mutableListOf<Double>()
        var numSamples = 0L

        rSocketRequester
            .route("fit")
            .data(
                Flux
                    .fromIterable(
                        parameters
                            .chunked(chunkSize)
                            .map(::FitIns),
                    ).apply {
                        client.limitRate?.let(::limitRate)
                    },
            ).retrieveFlux(FitRes::class.java)
            .doOnEach {
                it.get()?.let { res ->
                    status = res.status
                    if (status == RSocketStatus.NOOP) {
                        return@let
                    }
                    numSamples = res.numSamples
                    fitParameters.addAll(res.parameters)
                }
            }.blockLast()

        if (status == RSocketStatus.NOOP) {
            logger.info { "[RSocket]: Client sent data by other transport" }
            return
        }

        val messageStatus = MessageStatus.fromRSocket(status)
        messageQueue.add(
            Message(
                client,
                ClientFitResult(
                    messageStatus,
                    fitParameters,
                    numSamples,
                ),
            ),
        )
    }

    override fun evaluate(
        parameters: List<Double>,
        chunkSize: Int,
        timeout: Duration,
    ) {
        val res =
            rSocketRequester
                .route("evaluate")
                .data(
                    Flux
                        .fromIterable(parameters.chunked(chunkSize).map(::EvaluateIns))
                        .apply {
                            client.limitRate?.let(::limitRate)
                        },
                ).retrieveFlux(EvaluateRes::class.java)
                .blockFirst(timeout.toJavaDuration())!!

        if (res.status == RSocketStatus.NOOP) {
            logger.info { "[RSocket]: Client sent data by other transport" }
            return
        }

        messageQueue.add(
            Message(
                client,
                ClientEvaluateResult(
                    MessageStatus.fromRSocket(res.status),
                    res.loss,
                    res.numSamples,
                ),
            ),
        )
    }
}
