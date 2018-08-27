package main

import io.vertx.core.AbstractVerticle
import io.vertx.core.Vertx
import io.vertx.core.http.HttpClientOptions
import io.vertx.core.http.HttpMethod
import io.vertx.core.json.JsonObject
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.BodyHandler
import io.vertx.ext.web.handler.CorsHandler
import io.vertx.ext.web.handler.StaticHandler

class AbstractVerticleServer : AbstractVerticle() {

    override fun start() {
        val router = Router.router(vertx)

        router.route().handler(BodyHandler.create())

        router.route().handler(CorsHandler.create("*").allowedMethod(HttpMethod.GET))

        router.route().handler(StaticHandler.create())

        router.get("/doggo/:breed").handler { routingContext ->
            val breed = routingContext.request().getParam("breed")

            val options = HttpClientOptions(HttpClientOptions().setSsl(true).setTrustAll(true))

            val client = vertx.createHttpClient(options)

            client.getNow(443,"dog.ceo" , "/api/breed/$breed/images") { response ->
                when(response.statusCode()) {
                    200 -> {
                        response.bodyHandler { body ->
                            val filteredImages = body.toJsonObject().getJsonArray("message").list.subList(0,5)
                            routingContext
                                    .response()
                                    .setStatusCode(response.statusCode())
                                    .end(JsonObject().put("message", filteredImages).put("status","success").toString())
                        }
                    }
                    else -> {
                        routingContext
                                .response()
                                .setStatusCode(response.statusCode())
                                .end(response.statusMessage())
                    }
                }
            }
        }

        vertx.createHttpServer().requestHandler(router::accept).listen(9000) { result ->
            if(result.succeeded()) {
                println("${this.javaClass.name} successfully started: http://localhost:9000")
            }else{
                println("${this.javaClass.name} init failed due to: ${result.result()}")
            }
        }
    }
}

fun main(args: Array<String>) {
    Vertx.vertx().deployVerticle(AbstractVerticleServer())
}
