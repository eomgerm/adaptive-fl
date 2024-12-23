package com.github.eomgerm.adaptiveflserver

import com.github.eomgerm.adaptivefl.proto.Status
import com.github.eomgerm.adaptiveflserver.rsocket.RSocketStatus
import kotlin.time.Duration
import kotlin.time.Duration.Companion.minutes

interface CommClient {
    fun getParameters()

    fun fit(
        parameters: List<Double>,
        chunkSize: Int,
        timeout: Duration = 10.minutes,
    )

    fun evaluate(
        parameters: List<Double>,
        chunkSize: Int,
        timeout: Duration = 10.minutes,
    )
}

data class ClientParametersResult(
    val status: MessageStatus,
    val parameters: List<Double>,
)

data class ClientFitResult(
    val status: MessageStatus,
    val parameters: List<Double>,
    val numSamples: Long,
)

data class ClientEvaluateResult(
    val status: MessageStatus,
    val loss: Double,
    val numSamples: Long,
)

enum class MessageStatus {
    OK,
    NOOP,
    ERROR,
    ;

    companion object {
        fun fromGrpc(status: Status): MessageStatus =
            when (status) {
                Status.OK -> OK
                Status.NOOP -> NOOP
                Status.ERROR -> ERROR
                else -> throw IllegalArgumentException("Invalid status")
            }

        fun fromRSocket(status: RSocketStatus): MessageStatus =
            when (status) {
                RSocketStatus.OK -> OK
                RSocketStatus.NOOP -> NOOP
                RSocketStatus.ERROR -> ERROR
            }
    }
}
