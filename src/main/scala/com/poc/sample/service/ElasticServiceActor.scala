package com.poc.sample.service

import akka.actor.{Actor, ActorLogging}
import org.apache.http.util.EntityUtils
import org.elasticsearch.client.{Request, RestClient}


case class Employee(email: String, fr_email: String, phone: String, country: String)

class ElasticServiceActor(restClient: RestClient) extends Actor with ActorLogging {


  override def receive: Receive = {

    case elasticDomain: String => {
      val senderRef = sender()
      val request = new Request("GET", "foo/_search?format=json")
      val response = restClient.performRequest(request)
      val responseEntity = EntityUtils.toString(response.getEntity)
      senderRef ! responseEntity
    }
    case other =>
      sender() ! "Hey, wrong information being sent to me!!"


  }

}
