package com.poc.sample

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.http.scaladsl.Http
import akka.http.scaladsl.Http.ServerBinding
import akka.http.scaladsl.server.Directives
import akka.routing.FromConfig
import akka.stream.ActorMaterializer
import com.github.swagger.akka.SwaggerSite
import com.poc.sample.api.NTKRestService
import com.poc.sample.service.ElasticServiceActor
import com.poc.sample.swagger.SwaggerDocService
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.LazyLogging
import org.apache.http.HttpHost
import org.elasticsearch.client.RestClient

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

class NTKMain(implicit val system: ActorSystem, implicit val materializer: ActorMaterializer, implicit val elasticServiceActor: ActorRef) extends LazyLogging with Directives with SwaggerSite {

  def startServer(address: String, port: Int) = {
    val ntkAPIRoutes = new NTKRestService().ntkRoutes ~ new SwaggerDocService(address, port, system).routes ~ swaggerSiteRoute
    val binding: Future[ServerBinding] = Http().bindAndHandle(ntkAPIRoutes, address, port)
    logger.info("NTK service has been started in the port " + port)
    bindingErrorLog(binding, address, port)
  }

  def bindingErrorLog(binding: Future[ServerBinding], address: String, port: Int): Unit = {
    binding onFailure {
      case ex: Exception ⇒
        logger.error(s"Failed to bind NTK service to $address:$port!", ex)
    }
  }

}

object NTKMain {

  def main(args: Array[String]): Unit = {

    val httpElasticClient = RestClient.builder(
      new HttpHost("localhost", 9200, "http")).build()
    val config = ConfigFactory.load()
    implicit val actorSystem = ActorSystem("ClusterSystem")
    implicit val materializer = ActorMaterializer()
    implicit val elasticServiceActor = actorSystem.actorOf(Props(new ElasticServiceActor(httpElasticClient)).withRouter(FromConfig()), name = "ntkRouter")

    val host = config.getStringList("http.host").get(0)
    val port = config.getIntList("http.port").get(0)

    val server = new NTKMain()
    server.startServer(host, port)
  }

}
