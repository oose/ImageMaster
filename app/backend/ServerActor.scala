package backend

import akka.actor.ActorLogging
import akka.actor.Actor
import akka.pattern._
import play.api.libs.ws.WS
import scala.concurrent.Future
import play.api.libs.ws.Response
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.util.Try
import scala.util.Success
import scala.util.Failure
import play.api.libs.json._
import backend.Evaluation._

class ServerActor(url: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  def imageUrl = s"${url}/image"
  def pingUrl = s"${url}/ping"

  def receive = {
    case RequestId =>
      log.info(s"sender: $sender")
      log.info(s"requesting image url from: $imageUrl")
      WS.url(imageUrl).get pipeTo sender

    case Ping =>
      val response = Try(Await.result(WS.url(pingUrl).get, 5.seconds))
      response match {
        case Success(response) =>
          response.status match {
            case 200 => sender ! Pong(url)
            case _ => sender ! TimeoutPong(url)
          }
        case Failure(e) =>
          	sender ! TimeoutPong(url)
      }
      
    case e: Evaluation => 
      log.info(s"received evaluation for ${e.id} in $url")
      val s = sender
      if (e.id.startsWith(url)) {
        log.info(s"Accept evaluation for ${e.id} in $url")
        val json = Json.toJson(e)
        WS.url(imageUrl).post(json)
        
        sender ! s"ACK: $url"
      }
  }

}