package com.github.eomgerm.adaptiveflserver.grpc

import com.github.eomgerm.adaptivefl.proto.ClientJoinServiceGrpc.ClientJoinServiceImplBase
import com.github.eomgerm.adaptivefl.proto.JoinIns
import com.github.eomgerm.adaptivefl.proto.JoinRes
import com.github.eomgerm.adaptivefl.proto.Status
import com.github.eomgerm.adaptiveflserver.common.Client
import com.github.eomgerm.adaptiveflserver.common.ClientManager
import com.github.eomgerm.adaptiveflserver.config.ServerProperties
import io.github.oshai.kotlinlogging.KotlinLogging
import io.grpc.ManagedChannelBuilder
import io.grpc.stub.StreamObserver
import net.devh.boot.grpc.server.service.GrpcService

private val logger = KotlinLogging.logger {}

@GrpcService
class ClientJoinGrpcService(
    private val clientManager: ClientManager,
    private val serverProperties: ServerProperties,
) : ClientJoinServiceImplBase() {
    override fun join(
        request: JoinIns,
        responseObserver: StreamObserver<JoinRes>,
    ) {
        val client =
            Client(
                clientId = request.clientId,
                backPressureRequired = request.backPressureRequired,
                transports = request.transportsList,
                host = request.host,
                port = request.port,
                grpcChannel = ManagedChannelBuilder.forAddress(request.host, request.port).usePlaintext().build(),
            )

        clientManager.addClient(client)

        responseObserver.onNext(
            buildJoinRes(
                Status.OK,
                serverProperties.parametersThreshold,
                serverProperties.chunkSize,
            ),
        )

        responseObserver.onCompleted()

        logger.info { "Client joined: ${clientManager.getClient(request.clientId)?.clientId}" }
    }
}
