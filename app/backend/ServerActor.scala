package backend

import scala.util.Failure
import scala.util.Success
import akka.actor._
import akka.actor.actorRef2Scala
import akka.pattern.pipe
import backend.Evaluation.formats
import play.api.libs.json.Json
import play.api.libs.ws.WS
import akka.event.LoggingReceive
import akka.pattern.CircuitBreaker
import scala.concurrent.duration._
import akka.pattern.CircuitBreakerOpenException
import scala.concurrent.duration._

/**
 *  Represents a server serving images under url given in the constructor.
 *
 */
class ServerActor(url: String) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val imageUrl = s"${url}/image"
  val pingUrl = s"${url}/ping"
  val confUrl = s"${url}/conf"

  val breaker =
    new CircuitBreaker(context.system.scheduler,
      maxFailures = 3,
      callTimeout = 1 seconds,
      resetTimeout = 30 seconds).onOpen(circuitOpen()).onClose(circuitClosed())

  def circuitOpen() = {
    log.error("""
        Circuitbreaker is open
        
    """)
  }

  def circuitClosed() = {
    log.info("Circuitbreaker is closed")
  }

  def receive = LoggingReceive {
    case RequestImageId =>
      log.info(s"""
          sender: $sender
          requesting image url from: $imageUrl
         
      """)
      breaker.withCircuitBreaker(WS.url(imageUrl).get) pipeTo sender

    case Ping =>
      // keep the sender out of the closure in order 
      // to maintain thread safety.
      val currentSender = sender
      // request webservice from image server
      val responseFuture = WS.url(pingUrl).get

      // register nonblocking callback
      // Note: we need to use currentSender to send the message, as
      // the normal sender actor might not exist anymore once this
      // callback is called.
      breaker.withCircuitBreaker(responseFuture).onComplete {
        case Success(response) =>
          response.status match {
            case 200 => currentSender ! Pong(url)
            case _ => currentSender ! TimeoutPong(url)
          }
        case Failure(e) =>
          e match {
            case cboEx: CircuitBreakerOpenException =>
              val remainingSeconds = cboEx.remainingDuration.toUnit(MINUTES)
              log.info(s"""
                    CircuitBreakerOpenException: ${cboEx.getMessage()}
                    Remains open for ${remainingSeconds} minutes 
                    
                """)
              currentSender ! TimeoutPong(url, Some(cboEx.remainingDuration))
            case _ =>
              currentSender ! TimeoutPong(url)
          }
      }

    case e: Evaluation =>
      log.info(s"""
          received evaluation for ${e.id} in $url
          
      """)
      val s = sender
      if (e.id.startsWith(url)) {
        val json = Json.toJson(e)
        log.info(s"""
            Accept evaluation for ${e.id} in $url
            Json: ${Json.prettyPrint(json)}

        """)
        //forward the evaluation to the image server
        WS.url(imageUrl).post(json).onComplete {
          case Success(_) =>
            log.info(s"""
                Image successfully delivered to ${imageUrl}
            """)
          case Failure(e) =>
            log.error(s"""
                Problem encountered.
                Stacktrace: ${e.getStackTraceString}
                """)
        }
        sender ! s"ACK: $url"
      }
  }
}