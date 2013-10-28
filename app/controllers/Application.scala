package controllers

import scala.concurrent.Future

import akka.actor._
import akka.pattern.ask
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import backend.Evaluation
import backend.MasterActor
import backend.Ping
import backend.PingActor
import backend.PingMasterActor
import backend.PingResponse
import backend.RequestImageId
import backend.RequestWebSocket
import backend.WebSocketResponse
import common.config.Configured
import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.json.Json
import play.api.mvc._
import util.AppConfig

object Application extends Controller with Configured {

  lazy val appConfig = configured[AppConfig]

  val masterActor = Akka.system.actorOf(Props(new MasterActor(appConfig.serverNames)), "MasterActor")
  val pingActor = Akka.system.actorOf(Props(new PingActor(masterActor)), "PingActor")
  val pingMasterActor = Akka.system.actorOf(Props(new PingMasterActor(masterActor)), "PingMasterActor")

  implicit val timeout: Timeout = appConfig.defaultTimeout

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * retrieve an image by asking the MasterActor to route the request to any of the known
   *  servers and return the result.
   */
  def image = Action.async {

    val responseFuture = (masterActor ? RequestImageId).mapTo[play.api.libs.ws.Response]
    responseFuture.map(response =>
      response.status match {
        case 200 => Ok(response.body)
        case _ => BadRequest(response.body)
      }).recover {
      case connEx: Exception => (ServiceUnavailable(connEx.getMessage))
    }
  }

  def saveTags = Action.async(parse.json) {
    request =>
      Logger.info(s"received image evaluation: ${request.body}")
      val id = (request.body \ "id").asOpt[String]
      val tags = (request.body \ "tags").asOpt[List[String]]

      (id, tags) match {
        case (Some(id), Some(tags)) =>
          val response = (masterActor ? Evaluation(id, tags)).mapTo[String]
          response.map(r => Ok(r))

        case _ => Future { BadRequest }
      }
  }

  /**
   * request new websocket channels from the [[backend.PingMasterActor]]
   */ 
  def ws = WebSocket.async[JsValue] {
    request =>
      // request new websocket channels from the PingMasterActor
      val response = (pingMasterActor ? RequestWebSocket).mapTo[WebSocketResponse]
      response.map {
        case WebSocketResponse(in, out) =>
          (in, out)
      }
  }
}