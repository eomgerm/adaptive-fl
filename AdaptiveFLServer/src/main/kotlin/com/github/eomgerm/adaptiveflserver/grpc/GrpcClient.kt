package com.github.eomgerm.adaptiveflserver.grpc

import com.github.eomgerm.adaptivefl.proto.*
import com.github.eomgerm.adaptiveflserver.*
import com.github.eomgerm.adaptiveflserver.common.Client
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue.*
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.stub.StreamObserver
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.time.Duration

private val logger = KotlinLogging.logger {}

class GrpcClient(
    val client: Client,
    private val messageQueue: ClientMessageQueue,
    private val stub: FlWorkflowServiceGrpc.FlWorkflowServiceStub = FlWorkflowServiceGrpc.newStub(client.grpcChannel),
) : CommClient {
    override fun getParameters() {
        val latch = CountDownLatch(1)

        var status = Status.NOOP
        val parameters = mutableListOf<Double>()

        stub.initParameters(
            GetParametersIns.newBuilder().build(),
            object : StreamObserver<GetParametersRes> {
                override fun onNext(res: GetParametersRes) {
                    status = res.status
                    if (status == Status.NOOP) {
                        return
                    }
                    parameters.addAll(res.parameters.tensorsList)
                }

                override fun onError(t: Throwable) {
                    logger.error { t }
                    latch.countDown()
                }

                override fun onCompleted() {
                    latch.countDown()
                }
            },
        )

        latch.await()

        if (status == Status.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        messageQueue.add(
            Message(
                client,
                ClientParametersResult(MessageStatus.fromGrpc(status), parameters),
            ),
        )
    }

    override fun fit(
        parameters: List<Double>,
        chunkSize: Int,
        timeout: Duration,
    ) {
        val latch = CountDownLatch(1)

        var status = Status.NOOP
        val fitParameters = mutableListOf<Double>()
        var numSamples = 0L
        val requestObserver =
            stub
                .withDeadlineAfter(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .fit(
                    object : StreamObserver<FitRes> {
                        override fun onNext(res: FitRes) {
                            status = res.status
                            if (status == Status.NOOP) {
                                return
                            }
                            numSamples = res.numSamples
                            fitParameters.addAll(res.parameters.tensorsList)
                        }

                        override fun onError(t: Throwable) {
                            logger.error { t }
                            latch.countDown()
                        }

                        override fun onCompleted() {
                            latch.countDown()
                        }
                    },
                )

        parameters.chunked(chunkSize).forEach { chunked ->
            requestObserver.onNext(
                buildFitIns(chunked),
            )
        }
        requestObserver.onCompleted()

        latch.await()
        if (status == Status.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        messageQueue.add(
            Message(
                client,
                ClientFitResult(
                    MessageStatus.fromGrpc(status),
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
        val latch = CountDownLatch(1)

        var status = Status.NOOP
        var loss = 0.0
        var numSamples = 0L
        val requestObserver =
            stub
                .withDeadlineAfter(timeout.inWholeMilliseconds, TimeUnit.MILLISECONDS)
                .evaluate(
                    object : StreamObserver<EvaluateRes> {
                        override fun onNext(res: EvaluateRes) {
                            status = res.status
                            if (status == Status.NOOP) {
                                return
                            }

                            loss = res.loss
                            numSamples = res.numSamples
                        }

                        override fun onError(t: Throwable) {
                            logger.error { t }
                            latch.countDown()
                        }

                        override fun onCompleted() {
                            latch.countDown()
                        }
                    },
                )

        parameters.chunked(chunkSize).forEach { chunked ->
            requestObserver.onNext(
                buildEvaluateIns(chunked),
            )
        }
        requestObserver.onCompleted()

        latch.await()

        if (status == Status.NOOP) {
            logger.info { "Client sent data by other transport" }
            return
        }

        messageQueue.add(
            Message(
                client,
                ClientEvaluateResult(
                    MessageStatus.fromGrpc(status),
                    loss,
                    numSamples,
                ),
            ),
        )
    }
}
