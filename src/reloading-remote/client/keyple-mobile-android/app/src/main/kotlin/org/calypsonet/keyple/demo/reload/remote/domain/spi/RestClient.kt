package org.calypsonet.keyple.demo.reload.remote.domain.spi

import io.reactivex.Single
import org.eclipse.keyple.distributed.MessageDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST

/**
 * Cannot directly extend SyncEndpointClient because retrofit allows API interfaces to extend
 * interfaces.
 */
interface RestClient {

  @GET("/card/sam-status") fun ping(): Single<String>

  @Headers("Accept: application/json", "Content-Type: application/json; charset=UTF-8")
  @POST("/card/remote-plugin")
  fun sendRequest(@Body msg: MessageDto?): Single<MutableList<MessageDto>>
}