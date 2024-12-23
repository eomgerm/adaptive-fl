package com.github.eomgerm.adaptiveflclient.rsocket

import com.github.eomgerm.adaptivefl.proto.Transport
import com.github.eomgerm.adaptiveflclient.Model
import com.github.eomgerm.adaptiveflclient.common.TransportDecisionMaker
import com.github.eomgerm.adaptiveflclient.common.TransportDecisionMaker.DecisionParams
import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.config.ServerConfig
import com.github.eomgerm.adaptiveflclient.grpc.GrpcClient
import io.github.oshai.kotlinlogging.KotlinLogging
import org.springframework.messaging.handler.annotation.MessageMapping
import org.springframework.stereotype.Controller
import reactor.core.publisher.Flux

private val logger = KotlinLogging.logger {}

@Controller
class RSocketController(
    private val transportDecisionMaker: TransportDecisionMaker,
    private val grpcClient: GrpcClient,
    private val clientProperties: ClientProperties,
    private val serverConfig: ServerConfig,
) {
    @MessageMapping("initParams")
    fun initParams(): Flux<GetParametersRes> {
        logger.info { "Server requested initial parameters" }
        val parameters = Model.getParameters()

        val transport = transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))

        if (transport != Transport.RSOCKET) {
            grpcClient.respondInitParameters(parameters)
            return Flux.just(GetParametersRes(RSocketStatus.NOOP, emptyList(), clientProperties.id))
        }

        return Flux.fromIterable(
            parameters.chunked(serverConfig.chunkSize).map { chunked ->
                GetParametersRes(RSocketStatus.OK, chunked, clientProperties.id)
            },
        )
    }

    @MessageMapping("fit")
    fun fit(req: Flux<FitIns>): Flux<FitRes> {
        logger.info { "Server requested fit" }

        return req
            .collectList()
            .flatMapMany { fitInsList ->
                val parameters = fitInsList.flatMap { it.parameters }
                val (fitParameters, numSamples) = Model.fit(parameters)

                val transport = transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))

                if (transport != Transport.RSOCKET) {
                    grpcClient.respondFitResult(fitParameters, numSamples)
                    Flux.just(FitRes(RSocketStatus.NOOP, emptyList(), 0, clientProperties.id))
                } else {
                    Flux.fromIterable(
                        fitParameters.chunked(serverConfig.chunkSize).map { chunked ->
                            FitRes(RSocketStatus.OK, chunked, numSamples, clientProperties.id)
                        },
                    )
                }
            }
    }

    @MessageMapping("evaluate")
    fun evaluate(req: Flux<EvaluateIns>): Flux<EvaluateRes> {
        logger.info { "Server requested evaluate" }

        return req
            .collectList()
            .flatMapMany { evaluateInsList ->
                val parameters = evaluateInsList.flatMap { it.parameters }
                val (loss, numSamples) = Model.evaluate(parameters)

                val transport =
                    transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))

                if (transport != Transport.RSOCKET) {
                    grpcClient.respondEvaluateResult(loss, numSamples)
                    Flux.just(EvaluateRes(RSocketStatus.NOOP, 0.0, 0, clientProperties.id))
                } else {
                    Flux.just(EvaluateRes(RSocketStatus.OK, loss, numSamples, clientProperties.id))
                }
            }
    }
}
