package com.github.eomgerm.adaptiveflserver.grpc

import com.github.eomgerm.adaptivefl.proto.*
import com.github.eomgerm.adaptivefl.proto.FlWorkflowServiceGrpc.FlWorkflowServiceImplBase
import com.github.eomgerm.adaptiveflserver.ClientEvaluateResult
import com.github.eomgerm.adaptiveflserver.ClientFitResult
import com.github.eomgerm.adaptiveflserver.ClientParametersResult
import com.github.eomgerm.adaptiveflserver.MessageStatus
import com.github.eomgerm.adaptiveflserver.common.ClientManager
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue.Message
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

private val logger = KotlinLogging.logger {}

@GrpcService
class FlWorkflowGrpcService(
    private val messageQueue: ClientMessageQueue,
    private val clientManager: ClientManager,
) : FlWorkflowServiceImplBase() {
    override fun respondInitParameters(responseObserver: StreamObserver<TaskRes>): StreamObserver<GetParametersRes> =
        object : StreamObserver<GetParametersRes> {
            val parameters = mutableListOf<Double>()
            var clientId = ""
            var status = Status.NOOP

            override fun onNext(res: GetParametersRes) {
                parameters.addAll(res.parameters.tensorsList)
                clientId = res.clientId
                status = res.status
            }

            override fun onError(t: Throwable) {
                logger.error { t }
            }

            override fun onCompleted() {
                responseObserver.onNext(buildTaskRes())
                responseObserver.onCompleted()

                messageQueue.add(
                    Message(
                        clientManager.getClient(clientId)!!,
                        ClientParametersResult(
                            MessageStatus.fromGrpc(status),
                            parameters,
                        ),
                    ),
                )
            }
        }

    override fun respondFitRes(responseObserver: StreamObserver<TaskRes>): StreamObserver<FitRes> =
        object : StreamObserver<FitRes> {
            var status = Status.NOOP
            val parameters = mutableListOf<Double>()
            var clientId = ""
            var numSamples = 0L

            override fun onNext(res: FitRes) {
                status = res.status
                parameters.addAll(res.parameters.tensorsList)
                clientId = res.clientId
                numSamples = res.numSamples
            }

            override fun onError(t: Throwable) {
                logger.error { t }
            }

            override fun onCompleted() {
                responseObserver.onNext(buildTaskRes())
                responseObserver.onCompleted()

                messageQueue.add(
                    Message(
                        clientManager.getClient(clientId)!!,
                        ClientFitResult(
                            MessageStatus.fromGrpc(status),
                            parameters,
                            numSamples,
                        ),
                    ),
                )
            }
        }

    override fun respondEvaluateRes(
        request: EvaluateRes,
        responseObserver: StreamObserver<TaskRes>,
    ) {
        responseObserver.onNext(buildTaskRes())
        responseObserver.onCompleted()

        messageQueue.add(
            Message(
                clientManager.getClient(request.clientId)!!,
                ClientEvaluateResult(
                    MessageStatus.fromGrpc(request.status),
                    request.loss,
                    request.numSamples,
                ),
            ),
        )
    }
}
