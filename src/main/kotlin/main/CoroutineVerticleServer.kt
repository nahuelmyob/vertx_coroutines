package main

import io.vertx.core.Future
import io.vertx.core.Vertx
import io.vertx.core.buffer.Buffer
import io.vertx.core.http.HttpClient
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpClientResponse
import io.vertx.core.http.HttpMethod
import io.vertx.core.http.HttpServer
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.RoutingContext
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler
import io.vertx.kotlin.coroutines.CoroutineVerticle
import io.vertx.kotlin.coroutines.awaitEvent
import io.vertx.kotlin.coroutines.dispatcher
import kotlinx.coroutines.experimental.async
import kotlinx.coroutines.experimental.launch

class CoroutineVerticleServer : CoroutineVerticle() {
    suspend override fun start() {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET))

        router.route().handler(StaticHandler.create())

        router.get("/doggo/:breed").handler({ ctx -> launch(ctx.vertx().dispatcher()) { getBreedRouteHandler(ctx) } })

        val httpServer = createHttpServer(vertx, router)

        if(httpServer.succeeded()) {
            println("${this.javaClass.name} successfully started: http://localhost:9000")
        }else{
            println("${this.javaClass.name} init failed due to: ${httpServer.result()}")
        }

    }
}

suspend fun getBreedRouteHandler(routingContext: RoutingContext) {

    val options = HttpClientOptions(HttpClientOptions().setSsl(true).setTrustAll(true))

    val client = routingContext.vertx().createHttpClient(options)

    val breedImagesResponse = httpGetImages(client, routingContext.request().getParam("breed"))

    when (breedImagesResponse.statusCode()) {
        200 -> routingContext.response().setStatusCode(breedImagesResponse.statusCode()).end(createResponseModel(breedImagesResponse).toString())
        else -> routingContext.response().setStatusCode(breedImagesResponse.statusCode()).end(breedImagesResponse.statusMessage())
    }
}

suspend fun createResponseModel(response : HttpClientResponse) : JsonObject {
    val filteredImages = getBody(response)
            .toJsonObject()
            .getJsonArray("message")
            .list.subList(0,5)
    return JsonObject().put("message", filteredImages).put("status", "success")
}

fun createHttpServer(vertx: Vertx, router: Router) = Future.future<HttpServer> { h ->
    vertx.createHttpServer().requestHandler(router::accept).listen(9000, h)
}

suspend fun httpGetImages(client : HttpClient, breed : String) : HttpClientResponse = awaitEvent { h ->
    client.getNow(443,"dog.ceo" , "/api/breed/$breed/images", h)
}

suspend fun getBody(response : HttpClientResponse) : Buffer = awaitEvent { h ->
    response.bodyHandler(h)
}

fun main(args : Array<String>) {
    Vertx.vertx().deployVerticle(CoroutineVerticleServer())
}
