package com.github.eomgerm.adaptiveflclient.rsocket

import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.config.ServerConfig
import org.springframework.messaging.rsocket.RSocketRequester
import org.springframework.stereotype.Service
import reactor.core.publisher.Flux

@Service
class RSocketClient(
    private val rSocketRequester: RSocketRequester,
    private val clientProperties: ClientProperties,
    private val serverConfig: ServerConfig,
) {
    fun joinToServer() {
        rSocketRequester
            .route("join")
            .data(JoinIns(clientProperties.id, "localhost", 7001))
            .send()
            .block()
    }

    fun respondInitParameters(parameters: List<Double>) {
        rSocketRequester
            .route("initParameters")
            .data(
                Flux
                    .fromIterable(
                        parameters.chunked(serverConfig.chunkSize).map { chunked ->
                            GetParametersRes(
                                RSocketStatus.OK,
                                chunked,
                                clientProperties.id,
                            )
                        },
                    ),
            ).send()
            .log()
            .block()
    }

    fun respondFitResult(
        parameters: List<Double>,
        numSamples: Long,
    ) {
        rSocketRequester
            .route("fit")
            .data(
                Flux.fromIterable(
                    parameters.chunked(serverConfig.chunkSize).map { chunked ->
                        FitRes(
                            RSocketStatus.OK,
                            chunked,
                            numSamples,
                            clientProperties.id,
                        )
                    },
                ),
            ).send()
            .log()
            .block()
    }

    fun respondEvaluateResult(
        loss: Double,
        numSamples: Long,
    ) {
        rSocketRequester
            .route("evaluate")
            .data(
                EvaluateRes(
                    RSocketStatus.OK,
                    loss,
                    numSamples,
                    clientProperties.id,
                ),
            ).send()
            .log()
            .block()
    }
}
