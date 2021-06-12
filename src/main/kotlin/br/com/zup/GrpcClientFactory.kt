package br.com.zup

import br.com.edu.FretesServiceGrpc
import io.grpc.ManagedChannel
import io.micronaut.context.annotation.Factory
import io.micronaut.grpc.annotation.GrpcChannel
import javax.inject.Singleton

//Com essa classe o Micronaut agora poderá criar/injetar/gerenciar um objeto Cliente para consumir o Serviço FretesService (esse serviço é descrito no arquivo .proto)
@Factory
class GrpcClientFactory {

    @Singleton
    fun fretesClientStub(@GrpcChannel("fretes") channel: ManagedChannel): FretesServiceGrpc.FretesServiceBlockingStub {
        return FretesServiceGrpc.newBlockingStub(channel)
    }

}