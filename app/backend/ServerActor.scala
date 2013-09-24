package backend

import scala.util.Failure
import scala.util.Success

import akka.actor._
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import backend.Evaluation.formats
import play.api.libs.json.Json
import play.api.libs.ws.WS

class ServerActor(url: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val imageUrl = s"${url}/image"
  val pingUrl = s"${url}/ping"
  val confUrl = s"${url}/conf"

  def receive = {
    case RequestId =>
      log.info(s"sender: $sender")
      log.info(s"requesting image url from: $imageUrl")
      WS.url(imageUrl).get pipeTo sender

    case Ping =>
      // keep the sender out of the closure
      val tmpSender = sender
      // request webservice from image server
      val responseFuture = WS.url(pingUrl).get
      
      // register nonblocking callback
      responseFuture.onComplete {
        case Success(response) => 
          response.status match {
            case 200 => tmpSender ! Pong(url)
            case _ => tmpSender ! TimeoutPong(url)
          }
        case Failure(e) =>
          	tmpSender ! TimeoutPong(url)
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