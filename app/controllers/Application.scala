package controllers

import scala.concurrent.Future

import play.api._
import play.api.Play.current
import play.api.libs.concurrent.Akka
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json.JsValue
import play.api.libs.ws._
import play.api.mvc._

import akka.actor._
import akka.pattern.ask
import akka.util._
import akka.util.Timeout.durationToTimeout

import backend.Evaluation
import backend.MasterActor
import backend.PingActor
import backend.PingMasterActor
import backend.RequestImageId
import backend.RequestWebSocket
import backend.WebSocketResponse
import common.akka.AkkaUtil.createActor
import oose.play.config.Configured
import util.AppConfig

object Application extends Controller with Configured {

  lazy val appConfig = configured[AppConfig]
  import common.akka.AkkaUtil._
  implicit val actorSystem = Akka.system

  val masterActor = 
    createActor(Props(new MasterActor(appConfig.serverNames)), "MasterActor")
  val pingActor = 
    createActor(Props(new PingActor(masterActor)), "PingActor")
  val pingMasterActor = 
    createActor(Props(new PingMasterActor(masterActor)), "PingMasterActor")

  implicit val timeout: Timeout = appConfig.defaultTimeout
  

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * retrieve an image by asking the MasterActor to route the request to any of the known
   *  servers and return the result.
   */
  def image = Action.async {

    val responseFuture = (masterActor ? RequestImageId).mapTo[Response]
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
      Logger.info(s"""
          received image evaluation: ${request.body}
      
      """)
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
      val websocketFuture = (pingMasterActor ? RequestWebSocket).mapTo[WebSocketResponse]
      websocketFuture.map {
        case WebSocketResponse(in, out) =>
          (in, out)
      }
  }
}