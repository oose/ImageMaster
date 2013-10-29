package backend

import akka.actor._
import akka.routing.BroadcastRouter
import akka.routing.RoundRobinRouter
import akka.routing.ScatterGatherFirstCompletedRouter
import util.AppConfig
import common.config.Configured
import util.AppConfig
import akka.event.LoggingReceive

/**
 * The MasterActor contains a list of ServerActors which represent the external
 * servers for image delivery.
 * It can request new Image ids and helps in monitoring the external server status
 * by broadcasting ping requests to the ServerActors.
 *
 */
class MasterActor(serverNames: List[String]) extends Actor with ActorLogging with Configured {

  lazy val appConfig = configured[AppConfig]

  val serverActors: Map[String, ActorRef] =
    (serverNames.zipWithIndex.map {
      case (url, index) => (url, createServerActor(url, "ServerActor" + index))
    }).toMap

  val serverRoutees = serverActors.values.toVector

  val roundRobinRouter =
    context.actorOf(Props.empty.withRouter(
      RoundRobinRouter(routees = serverRoutees)),
      "RoundRobinRouter")

  val broadCastRouter =
    context.actorOf(Props.empty.withRouter(
      BroadcastRouter(routees = serverRoutees)),
      "BroadCastRouter")

  val scatterGatherRouter =
    context.actorOf(Props.empty.withRouter(
      ScatterGatherFirstCompletedRouter(routees = serverRoutees, within = appConfig.defaultTimeout)),
      "ScatterGatherRouter")

  private def createServerActor(url: String, name: String): ActorRef = {
    context.actorOf(Props(new ServerActor(url)), name)
  }

  def receive = LoggingReceive {
    case RequestImageId =>
      log.info("""
            ----> NEW REQUEST FROM CLIENT FOR AN IMAGE.
            forwarding RequestId to roundRobinRouter
            
        """)
      roundRobinRouter forward RequestImageId

    case e: Evaluation =>
      log.info("""
            forwarding evaluation to scatterGatherRouter
            
        """)
      scatterGatherRouter forward e

    case Ping =>
      log.info("""
            forwarding Ping to broadcastRouter
            
        """)
      broadCastRouter forward Ping
  }

}