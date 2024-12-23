package com.github.eomgerm.adaptiveflclient.rsocket

data class JoinIns(
    val clientId: String,
    val host: String,
    val port: Int,
)

data class JoinRes(
    val status: RSocketStatus,
    val message: String,
)

enum class RSocketStatus {
    OK,
    ERROR,
    NOOP,
}

data class GetParametersRes(
    val status: RSocketStatus,
    val parameters: List<Double>,
    val clientId: String,
)

data class FitIns(
    val parameters: List<Double>,
)

data class FitRes(
    val status: RSocketStatus,
    val parameters: List<Double>,
    val numSamples: Long,
    val clientId: String,
)

data class EvaluateIns(
    val parameters: List<Double>,
)

data class EvaluateRes(
    val status: RSocketStatus,
    val loss: Double,
    val numSamples: Long,
    val clientId: String,
)
