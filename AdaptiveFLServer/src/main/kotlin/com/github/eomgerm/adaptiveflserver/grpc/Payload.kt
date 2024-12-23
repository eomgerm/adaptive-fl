package com.github.eomgerm.adaptiveflserver.grpc

import com.github.eomgerm.adaptivefl.proto.*

fun buildJoinRes(
    status: Status,
    parametersThreshold: Long,
    chunkSize: Int,
) = joinRes {
    this.status = status
    this.parametersThreshold = parametersThreshold
    this.chunkSize = chunkSize
}

fun buildFitIns(parameters: List<Double>) =
    fitIns {
        this.parameters =
            parameters { tensors.addAll(parameters) }
    }

fun buildEvaluateIns(parameters: List<Double>) =
    evaluateIns {
        this.parameters =
            parameters { tensors.addAll(parameters) }
    }

fun buildTaskRes() =
    taskRes {
        this.status = Status.OK
        this.message = "OK"
    }
