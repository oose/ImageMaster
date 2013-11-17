package backend

import scala.concurrent.duration._
import scala.util.Failure
import scala.util.Success

import play.api.libs.json._
import play.api.libs.ws.WS

import akka.actor._
import akka.actor.actorRef2Scala
import akka.event.LoggingReceive
import akka.pattern._
import akka.pattern.pipe

import backend.Implicits.formats

import _root_.util.ApplicationConfiguration

/**
 *  Represents a server serving images.
 *
 */
class ServerActor(val url: String, val appConfig: ApplicationConfiguration) extends Actor with ActorLogging {

  implicit val ec = context.dispatcher

  val imageUrl = s"${url}/image"
  val pingUrl = s"${url}/ping"
  val confUrl = s"${url}/conf"

  val requestService = appConfig.service
  
  val breaker: CircuitBreaker =
    new CircuitBreaker(
      context.system.scheduler,
      maxFailures = 3,
      callTimeout = 1 seconds,
      resetTimeout = 30 seconds)
      .onOpen(circuitOpen())
      .onClose(circuitClosed())

  private def circuitOpen(): Unit = {
    log.error("""
        Circuitbreaker is open
        
    """)
  }

  private def circuitClosed(): Unit = {
    log.info("""
        Circuitbreaker is closed
        
    """)
  }

  def receive = LoggingReceive {
    case RequestImageId =>
      log.info(s"""
          sender: $sender
          requesting image url from: $imageUrl
         
      """)
      breaker.withCircuitBreaker(requestService.requestImage(imageUrl)) pipeTo sender

    case Ping =>
      // keep the sender out of the closure in order 
      // to maintain thread safety.
      val currentSender = sender
      // request webservice from image server
      val pingFuture = requestService.ping(pingUrl)

      // register nonblocking callback
      // Note: we need to use currentSender to send the message, as
      // the normal sender actor might not exist anymore once this
      // callback is called.
      breaker.withCircuitBreaker(pingFuture)
        .onComplete {
          case Success(response) =>
            response.status match {
              case 200 => currentSender ! Pong(url)
              case _ => currentSender ! TimeoutPong(url)
            }
          case Failure(e) =>
            e match {
              case cboEx: CircuitBreakerOpenException =>
                val remainingSeconds = cboEx.remainingDuration.toSeconds
                log.info(s"""
                    CircuitBreakerOpenException: ${cboEx.getMessage()}
                    Remains open for ${remainingSeconds} seconds 
                    
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
        // TODO refactor and create reasonable error messages.
        WS.url(imageUrl).post(json)
          .onComplete {
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