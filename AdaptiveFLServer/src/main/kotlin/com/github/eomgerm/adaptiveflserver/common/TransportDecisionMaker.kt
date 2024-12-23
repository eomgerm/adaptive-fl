package com.github.eomgerm.adaptiveflserver.common

import com.github.eomgerm.adaptivefl.proto.Transport
import com.github.eomgerm.adaptiveflserver.config.ServerProperties

open class TransportDecisionMaker(
    private val serverProperties: ServerProperties,
) {
    fun decideTransport(decisionParams: DecisionParams): Transport {
        if (decisionParams.availableTransports.size == 1) {
            return decisionParams.availableTransports.first()
        }

        if (decisionParams.backPressureRequired) {
            return Transport.RSOCKET
        }

        var score = 0

        if (decisionParams.parametersSize > serverProperties.parametersThreshold) {
            score++
        } else {
            score--
        }

        if (decisionParams.parametersSize / serverProperties.chunkSize < 2500) {
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

    class DecisionParams(
        val parametersSize: Int,
        val availableTransports: List<Transport>,
        val backPressureRequired: Boolean,
    )
}
