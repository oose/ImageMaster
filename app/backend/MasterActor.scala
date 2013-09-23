package backend

import akka.routing._
import scala.concurrent.duration._
import play.api.Play
import akka.actor.ActorLogging
import akka.actor.Actor
import akka.actor.ActorRef
import akka.actor.Props

class MasterActor(serverNames : List[String]) extends Actor with ActorLogging {
  

  val serverActors : Map[String,ActorRef] =
    (serverNames.zipWithIndex.map {
      case (url, index) => (url, createServerActor(url, "Client" + index))
    }).toMap
    
  def serverRoutees = serverActors.values.toVector
 
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
        ScatterGatherFirstCompletedRouter(routees = serverRoutees, within = 5.seconds)), 
        "ScatterGatherRouter")

  def createServerActor(url: String, name: String): ActorRef = {
    context.actorOf(Props(new ServerActor(url)), name)
  }

  def receive =
    {
      case RequestId =>
        log.info("forwarding RequestId to roundRobinRouter")
        roundRobinRouter forward RequestId

      case  e :Evaluation =>
        log.info("forwarding evaluation to scatterGatherRouter")
        scatterGatherRouter forward e
        
      case Ping =>
        log.info("forwarding Ping to broadcastRouter")
        broadCastRouter forward Ping
    }

}