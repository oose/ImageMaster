package controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._

import akka.actor.Props
import akka.pattern._
import akka.util.Timeout
import backend._
import backend.PingActor
import play.api._
import play.api.Play.current
import play.api.libs.concurrent._
import play.api.libs.concurrent.Execution.Implicits._
import play.api.libs.json._
import play.api.libs.ws._
import play.api.mvc._
import scala.collection.JavaConversions._

object Application extends Controller {

  val serverNames =
    Play.current.configuration.getStringList("image.server").get.toList

  val masterActor = Akka.system.actorOf(Props(new MasterActor(serverNames)), "MasterActor")
  val pingActor = Akka.system.actorOf(Props(new PingActor(masterActor)), "PingActor")
  val pingMasterActor = Akka.system.actorOf(Props(new PingMasterActor(masterActor)), "PingMasterActor")

  implicit val timeout: Timeout = 5.seconds

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * ping the state of all server machines.
   * Not Async!
   */
  def ping = Action {
    val response = Await.result((pingActor ? Ping).mapTo[List[PingResponse]], 5.seconds)
    Ok(Json.toJson(response))
  }

  /**
   * retrieve an image by asking the MasterActor to route the request to any of the known
   *  servers and return the result.
   */
  def image = Action.async {

    val response = (masterActor ? RequestId).mapTo[play.api.libs.ws.Response]
    response.map(response =>
      response.status match {
        case 200 => Ok(response.body)
        case _ => BadRequest(response.body)
      }).recover {
      case connEx: Exception => (ServiceUnavailable(connEx.getMessage))
    }

  }

  def saveTags = Action.async(parse.json) {
    request =>
      val id = (request.body \ "id").asOpt[String]
      val tags = (request.body \ "tags").asOpt[List[String]]

      (id, tags) match {
        case (Some(id), Some(tags)) =>

          val response = (masterActor ? Evaluation(id, tags)).mapTo[String]
          response.map(r => Ok(r))

        case _ => Future { BadRequest }
      }
  }

  def ws = WebSocket.async[JsValue] {
    request =>
      val response = (pingMasterActor ? RequestWebSocket).mapTo[WebSocketResponse]
      response.map {
        case WebSocketResponse(in, out) =>
          (in, out)
      }
  }

}