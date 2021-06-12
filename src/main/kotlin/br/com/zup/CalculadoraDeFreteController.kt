package br.com.zup

import br.com.edu.ErrorDetails
import br.com.edu.FreteRequest
import br.com.edu.FretesServiceGrpc
import com.google.protobuf.Any
import io.grpc.Status
import io.grpc.StatusRuntimeException
import io.grpc.protobuf.StatusProto
import io.micronaut.http.HttpStatus
import io.micronaut.http.annotation.Controller
import io.micronaut.http.annotation.Get
import io.micronaut.http.annotation.QueryValue
import io.micronaut.http.exceptions.HttpStatusException
import javax.inject.Inject

//Só será possível fazer o @Inject do objeto, pois criamos a classe GrpcClientFactory!
@Controller
class CalculadoraDeFreteController(@Inject val gRpcClient: FretesServiceGrpc.FretesServiceBlockingStub) {

    @Get("/api/fretes")
    fun calcula(@QueryValue(defaultValue = "") cep: String): FreteDto {

        val request = FreteRequest.newBuilder()
            .setCep(cep)
            .build()

        try {
            val response = gRpcClient.calculaFrete(request)
            return FreteDto(response.cep, response.valor)
        } catch (e: StatusRuntimeException) {

            //Pegando a descrição do erro que chegou;
            val description = e.status.description

            //Pegando o status do erro que chegou;
            val statusCode = e.status.code

            //Verificando se o erro que aconteceu foi INVALID_ARGUMENT (Verifificamos pelo CODE)
            if (statusCode == Status.Code.INVALID_ARGUMENT) {
                //Se ocorreu erro do tipo INVALID_ARGUMENT vamos retornar bad request para usuário!
                throw  HttpStatusException(HttpStatus.BAD_REQUEST, description)
            }

            //Agora vamos tratar o erro de segurança que criamos lá no servidor (quando enviamos CEP com fim 333
            //Sabemos que esse erro retorna um objeto que descreve melhor o erro, teremos que pegar esse objeto!

            //Verificando se o erro que aconteceu foi PERMISSION_DENIED (Verifificamos pelo CODE)
            if (statusCode == Status.Code.PERMISSION_DENIED) {

                //Verifico se no erro está vindo mais detalhes, como um objeto!
                val statusProto = StatusProto.fromThrowable(e)

                //Caso não tenha mais detalhes eu lanço um erro
                if(statusProto == null){
                    throw  HttpStatusException(HttpStatus.FORBIDDEN, description)
                }

                //Caso venha mais detalhes vou pegá-los:
                //Temos que tipar com Any do protobuf e não do Kotlin!
                //Estamos pegando o erro empacotado!
                val anyDetails:Any = statusProto.detailsList.get(0)

                //Desempacotando o erro para erroDetails
                val erroDetails = anyDetails.unpack(ErrorDetails::class.java)

                throw  HttpStatusException(HttpStatus.FORBIDDEN, "${erroDetails.code}: ${erroDetails.message}")
            }

            //Será o erro genérico que vamos lançar para o usuário...
            throw HttpStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.message)
        }
    }
}

data class FreteDto(
    val cep: String,
    val frete: Double
) {
}