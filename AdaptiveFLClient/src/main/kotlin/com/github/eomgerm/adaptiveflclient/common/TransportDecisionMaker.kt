package com.github.eomgerm.adaptiveflclient.common

import com.github.eomgerm.adaptivefl.proto.Transport
import com.github.eomgerm.adaptiveflclient.config.ClientProperties
import com.github.eomgerm.adaptiveflclient.config.ServerConfig
import org.springframework.stereotype.Component

@Component
class TransportDecisionMaker(
    private val serverConfig: ServerConfig,
    private val clientProperties: ClientProperties,
) {
    fun decideTransport(decisionParams: DecisionParams): Transport {
        if (clientProperties.transports.size == 1) {
            return clientProperties.transports.first()
        }

        var score = 0

        if (decisionParams.parametersSize > serverConfig.parametersThreshold) {
            score++
        } else {
            score--
        }

        if (decisionParams.parametersSize / serverConfig.chunkSize < 2500) {
            score += 2
        } else {
            score -= 2
        }

        return if (score > 0) {
            Transport.RSOCKET
        } else {
            Transport.GRPC
        }
    }

    data class DecisionParams(
        val parametersSize: Long,
    )
}
