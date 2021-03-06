package backend

import akka.actor._
import akka.event.LoggingReceive
import akka.routing._

import util.ApplicationConfiguration

/**
 * The MasterActor contains a list of ServerActors which represent the external
 * servers for image delivery.
 * It can request new Image ids and helps in monitoring the external server status
 * by broadcasting ping requests to the ServerActors.
 *
 */
class MasterActor(val appConfig: ApplicationConfiguration) extends Actor with ActorLogging {

  /**
   * The [[Map]] of servers with their URL as key and the responsible Actor as [[akka.actor.ActorRef]].
   */
  val serverActors: Map[String, ActorRef] = {
    def createIndex = appConfig.serverNames.zipWithIndex

    def createServerTuple: PartialFunction[(String, Int), (String, ActorRef)] = {
      case (url, index) => (url, createServerActor(url, "ServerActor" + index))
    }

    createIndex
      .map { createServerTuple(_) } toMap
  }

  val serverRoutees: Vector[ActorRef] = serverActors.values.toVector

  val roundRobinRouter: ActorRef =
    context.actorOf(Props.empty.withRouter(
      RoundRobinRouter(routees = serverRoutees)),
      "RoundRobinRouter")

  val broadCastRouter: ActorRef =
    context.actorOf(Props.empty.withRouter(
      BroadcastRouter(routees = serverRoutees)),
      "BroadCastRouter")

  val scatterGatherRouter: ActorRef =
    context.actorOf(Props.empty.withRouter(
      ScatterGatherFirstCompletedRouter(routees = serverRoutees, within = appConfig.defaultTimeout)),
      "ScatterGatherRouter")

  private def createServerActor(url: String, name: String): ActorRef = {
    context.actorOf(Props(new ServerActor(url, appConfig)), name)
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