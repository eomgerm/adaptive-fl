package com.github.eomgerm.adaptiveflserver

import com.github.eomgerm.adaptivefl.proto.Transport
import com.github.eomgerm.adaptiveflserver.common.Client
import com.github.eomgerm.adaptiveflserver.common.ClientMessageQueue
import com.github.eomgerm.adaptiveflserver.common.TransportDecisionMaker
import com.github.eomgerm.adaptiveflserver.config.ServerProperties
import com.github.eomgerm.adaptiveflserver.grpc.GrpcClient
import com.github.eomgerm.adaptiveflserver.rsocket.RSocketClient
import kotlin.time.Duration

class AdaptiveClient(
    val client: Client,
    private val serverProperties: ServerProperties,
    private val messageQueue: ClientMessageQueue,
) : TransportDecisionMaker(serverProperties) {
    fun getParameters() {
        val client =
            getClient(
                decideTransport(
                    DecisionParams(
                        0,
                        client.transports,
                        this.client.backPressureRequired,
                    ),
                ),
            )
        client.getParameters()
    }

    fun fit(
        parameters: List<Double>,
        timeout: Duration? = null,
    ) {
        val client =
            getClient(
                decideTransport(
                    DecisionParams(
                        parameters.size,
                        client.transports,
                        this.client.backPressureRequired,
                    ),
                ),
            )
        if (timeout != null) {
            client.fit(parameters, serverProperties.chunkSize, timeout)
        } else {
            client.fit(parameters, serverProperties.chunkSize)
        }
    }

    fun evaluate(
        parameters: List<Double>,
        timeout: Duration? = null,
    ) {
        val client =
            getClient(
                decideTransport(
                    DecisionParams(
                        parameters.size,
                        client.transports,
                        this.client.backPressureRequired,
                    ),
                ),
            )

        if (timeout == null) {
            client.evaluate(parameters, serverProperties.chunkSize)
        } else {
            client.evaluate(parameters, serverProperties.chunkSize, timeout)
        }
    }

    private fun getClient(transport: Transport): CommClient =
        when (transport) {
            Transport.GRPC -> GrpcClient(client, messageQueue)
            Transport.RSOCKET -> RSocketClient(client, messageQueue)
            else -> throw IllegalArgumentException("Invalid transport")
        }
}
