package com.poc.sample.swagger

import akka.actor.ActorSystem
import akka.stream.ActorMaterializer
import com.github.swagger.akka.model.Info
import com.github.swagger.akka.{HasActorSystem, SwaggerHttpService}
import com.poc.sample.api.NTKRestService

import scala.reflect.runtime.{universe => ru}

class SwaggerDocService(address: String, port: Int, system: ActorSystem) extends SwaggerHttpService with HasActorSystem {
  override implicit val actorSystem: ActorSystem = system
  override implicit val materializer: ActorMaterializer = ActorMaterializer()
  override val apiTypes = Seq(ru.typeOf[NTKRestService])
  override val host = address + ":" + port
  override val basePath = "/"
  override val apiDocsPath = "api-docs"
  override val info = Info(description = "Swagger docs for NTK service", version = "1.0", title = "NTK API", termsOfService = "NTK API terms and conditions")
}
