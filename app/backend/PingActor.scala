package backend

import scala.concurrent.ExecutionContext
import scala.concurrent.duration.DurationInt

import play.api.libs.iteratee.Concurrent
import play.api.libs.iteratee.Iteratee
import play.api.libs.json._

import akka.actor._
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive

import oose.play.config.Configured
import _root_.util.AppConfig
import _root_.util.Implicits.pingResponseWrite

class PingMasterActor(masterActor: ActorRef) extends Actor with ActorLogging {

  // list of PingActor ActorRefs.  One per websocket client.
  var children: List[ActorRef] = List.empty[ActorRef]

  // handle a javascript message from the websocket.
  private def handleJsMessage(msg: JsValue): Unit = {
    // just forward it on to all the children
    children.foreach { child =>
      child ! msg
    }
  }

  def receive = LoggingReceive {
    case msg: JsValue =>
      handleJsMessage(msg)

    case RequestWebSocket =>
      // This is called when a new client connects to the websocket.  We create
      // a new child Actor and forward this message on to it.
      log.info("""
          got a request for a new websocket
      
      """)
      val pingActor: ActorRef = context.actorOf(Props(new PingActor(masterActor)))
      children = pingActor :: children
      pingActor forward RequestWebSocket

    case Quit =>
      // A client has disconnected from the websocket
      log.info("""
          Got a quit message, removing child
          
      """")
      children = children.filterNot(_ == sender)
      context.stop(sender)

  }
}

class PingActor(masterActor: ActorRef) extends Actor with ActorLogging with Configured {

  implicit val ec: ExecutionContext = context.dispatcher

  val appConfig = configured[AppConfig]

  val (out, outChannel) = Concurrent.broadcast[JsValue]

  // This handles any messages sent from the browser to the server over the socket
  val in = Iteratee.foreach[JsValue] { message =>
    // just take the socket data and send it as an akka message to our parent
    context.parent ! message
  }.map { _ =>
    // tell the parent we've quit
    context.parent ! Quit
  }
  
  override def postStop() {
    log.info("""
        PingActor stopped.
        
    """)
  }

  def receive = LoggingReceive {
    case Ping =>
      masterActor ! Ping

    case pr: PingResponse =>
      
      log.info(s"""
          received ping response $pr
          pushing result to the websocket channel
          
      """)
      outChannel.push(Json.toJson(pr))

    case RequestWebSocket =>
      sender ! WebSocketResponse(in, out)
      context.system.scheduler.schedule(1.seconds, appConfig.pingRepeat, self, Ping)
  }

}