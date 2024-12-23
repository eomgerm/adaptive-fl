package com.github.eomgerm.adaptiveflclient.grpc

import com.github.eomgerm.adaptivefl.proto.*
import com.github.eomgerm.adaptivefl.proto.ClientJoinServiceGrpc.ClientJoinServiceBlockingStub
import com.github.eomgerm.adaptivefl.proto.FlWorkflowServiceGrpc.FlWorkflowServiceStub
import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.config.ServerConfig
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.client.inject.GrpcClient
import org.springframework.stereotype.Service

private val logger = KotlinLogging.logger {}

@Service
class GrpcClient(
    private val serverConfig: ServerConfig,
    private val clientProperties: ClientProperties,
    @GrpcClient("ClientJoinService") private val joinClient: ClientJoinServiceBlockingStub,
    @GrpcClient("FlWorkflowService") private val flWorkflowClient: FlWorkflowServiceStub,
) {
    fun joinToServer() {
        val request =
            buildJoinIns(
                clientId = clientProperties.id,
                backPressureRequired = clientProperties.backPressure.required,
                limitRate = clientProperties.backPressure.limitRate,
                transports = clientProperties.transports,
                host = clientProperties.grpc.host,
                port = clientProperties.grpc.port,
            )
        val response = joinClient.join(request)
        serverConfig.apply {
            parametersThreshold = response.parametersThreshold
            chunkSize = response.chunkSize
        }

        logger.info { "[GRPC] Successfully joined to server." }
        logger.info { "Server Config: parametersThreshold: ${serverConfig.parametersThreshold} chunkSize: ${serverConfig.chunkSize}" }
    }

    fun respondInitParameters(parameters: List<Double>) {
        val requestObserver =
            flWorkflowClient.respondInitParameters(
                object : StreamObserver<TaskRes> {
                    override fun onNext(res: TaskRes) {
                    }

                    override fun onError(t: Throwable) {
                        logger.error { t }
                    }

                    override fun onCompleted() {
                    }
                },
            )

        parameters.chunked(serverConfig.chunkSize).forEach { chunked ->
            requestObserver.onNext(
                buildGetParametersRes(Status.OK, chunked, clientProperties.id),
            )
        }
        requestObserver.onCompleted()
    }

    fun respondFitResult(
        parameters: List<Double>,
        numSamples: Long,
    ) {
        val requestObserver =
            flWorkflowClient.respondFitRes(
                object : StreamObserver<TaskRes> {
                    override fun onNext(res: TaskRes) {
                    }

                    override fun onError(t: Throwable) {
                        logger.error { t }
                    }

                    override fun onCompleted() {
                    }
                },
            )

        parameters.chunked(serverConfig.chunkSize).forEach { chunked ->
            requestObserver.onNext(
                buildFitRes(Status.OK, chunked, numSamples, clientProperties.id),
            )
        }
        requestObserver.onCompleted()
    }

    fun respondEvaluateResult(
        loss: Double,
        numSamples: Long,
    ) {
        val request = buildEvaluateRes(Status.OK, loss, numSamples, clientProperties.id)
        flWorkflowClient.respondEvaluateRes(
            request,
            object : StreamObserver<TaskRes> {
                override fun onNext(res: TaskRes) {
                }

                override fun onError(t: Throwable) {
                    logger.error { t }
                }

                override fun onCompleted() {
                }
            },
        )
    }
}
