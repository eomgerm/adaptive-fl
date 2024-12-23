package com.github.eomgerm.adaptiveflclient.grpc

import com.github.eomgerm.adaptivefl.proto.*

fun buildJoinIns(
    clientId: String,
    backPressureRequired: Boolean,
    limitRate: Int,
    transports: List<Transport>,
    host: String,
    port: Int,
) = joinIns {
    this.clientId = clientId
    this.backPressureRequired = backPressureRequired
    this.limitRate = limitRate
    this.transports.addAll(transports)
    this.host = host
    this.port = port
}

fun buildGetParametersRes(
    status: Status,
    parameters: List<Double>,
    clientId: String,
) = getParametersRes {
    this.status = status
    this.parameters =
        parameters {
            tensors.addAll(parameters)
        }
    this.clientId = clientId
}

fun buildFitRes(
    status: Status,
    parameters: List<Double>,
    numSamples: Long,
    clientId: String,
) = fitRes {
    this.status = status
    this.parameters = parameters { tensors.addAll(parameters) }
    this.numSamples = numSamples
    this.clientId = clientId
}

fun buildEvaluateRes(
    status: Status,
    loss: Double,
    numSamples: Long,
    clientId: String,
) = evaluateRes {
    this.status = status
    this.loss = loss
    this.numSamples = numSamples
    this.clientId = clientId
}
