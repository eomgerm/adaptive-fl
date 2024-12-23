package com.github.eomgerm.adaptiveflclient.grpc

import com.github.eomgerm.adaptivefl.proto.*
import com.github.eomgerm.adaptivefl.proto.FlWorkflowServiceGrpc.FlWorkflowServiceImplBase
import com.github.eomgerm.adaptiveflclient.Model
import com.github.eomgerm.adaptiveflclient.common.TransportDecisionMaker
import com.github.eomgerm.adaptiveflclient.common.TransportDecisionMaker.DecisionParams
import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.config.ServerConfig
import com.github.eomgerm.adaptiveflclient.rsocket.RSocketClient
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

private val logger = KotlinLogging.logger {}

@GrpcService
class FlWorkflowGrpcService(
    private val clientProperties: ClientProperties,
    private val transportDecisionMaker: TransportDecisionMaker,
    private val serverConfig: ServerConfig,
    private val rSocketClient: RSocketClient,
) : FlWorkflowServiceImplBase() {
    override fun initParameters(
        request: GetParametersIns,
        responseObserver: StreamObserver<GetParametersRes>,
    ) {
        val parameters = Model.getParameters()

        val transport = transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))
        if (transport != Transport.GRPC) {
            responseObserver.onNext(
                buildGetParametersRes(
                    Status.NOOP,
                    emptyList(),
                    clientProperties.id,
                ),
            )

            rSocketClient.respondInitParameters(parameters)
        } else {
            parameters.chunked(serverConfig.chunkSize).forEach { chunked ->
                responseObserver.onNext(
                    buildGetParametersRes(
                        Status.OK,
                        chunked,
                        clientProperties.id,
                    ),
                )
            }
        }

        logger.info { "[INIT_PARAMETERS]: parameters ${parameters.take(10)}..." }

        responseObserver.onCompleted()
    }

    override fun fit(responseObserver: StreamObserver<FitRes>): StreamObserver<FitIns> =
        object : StreamObserver<FitIns> {
            val parameters = mutableListOf<Double>()

            override fun onNext(req: FitIns) {
                parameters.addAll(req.parameters.tensorsList)
            }

            override fun onError(t: Throwable) {
                logger.error { t }
                responseObserver.onError(t)
            }

            override fun onCompleted() {
                logger.info { "[FIT]: Start fitting..." }
                val (parameters, numSamples) = Model.fit(parameters)

                val transport = transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))

                if (transport != Transport.GRPC) {
                    responseObserver.onNext(
                        buildFitRes(
                            Status.NOOP,
                            emptyList(),
                            0,
                            clientProperties.id,
                        ),
                    )

                    rSocketClient.respondFitResult(parameters, numSamples)
                } else {
                    parameters.chunked(serverConfig.chunkSize).forEach { chunked ->
                        responseObserver.onNext(
                            buildFitRes(
                                Status.OK,
                                chunked,
                                numSamples,
                                clientProperties.id,
                            ),
                        )
                    }
                }

                responseObserver.onCompleted()
                logger.info { "[FIT]: parameters ${parameters.take(10)}..." }
            }
        }

    override fun evaluate(responseObserver: StreamObserver<EvaluateRes>): StreamObserver<EvaluateIns> =
        object : StreamObserver<EvaluateIns> {
            val parameters = mutableListOf<Double>()

            override fun onNext(req: EvaluateIns) {
                parameters.addAll(req.parameters.tensorsList)
            }

            override fun onError(t: Throwable) {
                logger.error { t }
                responseObserver.onError(t)
            }

            override fun onCompleted() {
                logger.info { "[EVALUATE]: Start evaluating..." }

                val (loss, numSamples) = Model.evaluate(parameters)

                val transport = transportDecisionMaker.decideTransport(DecisionParams(Model.MODEL_SIZE))

                if (transport != Transport.GRPC) {
                    responseObserver.onNext(
                        buildEvaluateRes(
                            Status.NOOP,
                            0.0,
                            0,
                            clientProperties.id,
                        ),
                    )

                    rSocketClient.respondEvaluateResult(loss, numSamples)
                } else {
                    responseObserver.onNext(
                        buildEvaluateRes(
                            Status.OK,
                            loss,
                            numSamples,
                            clientProperties.id,
                        ),
                    )
                }

                responseObserver.onCompleted()
                logger.info { "[EVALUATE]: Loss: $loss Num of samples: $numSamples" }
            }
        }
}
