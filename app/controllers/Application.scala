package controllers

import java.net.ConnectException

import scala.concurrent.Future

import org.springframework.context.ApplicationContext

import play.api._
import play.api.libs.concurrent.Execution.Implicits.defaultContext
import play.api.libs.json._
import play.api.libs.ws.Response
import play.api.mvc._

import akka.actor._
import akka.pattern.ask
import akka.util._
import akka.util.Timeout.durationToTimeout

import backend._

import oose.play.actions.CorsAction
import oose.play.json.StatusMessage.error
import oose.play.json.StatusMessage.success
import oose.play.json.StatusMessage.writeableOf_StatusMessage
import oose.play.springextension.SpringExtensionImpl
import _root_.util.ApplicationConfiguration


class Application(val appConfig : ApplicationConfiguration)(implicit val actorSystem: ActorSystem, implicit val ctx: ApplicationContext) extends Controller {
  
  val springExtension = SpringExtensionImpl(actorSystem)
  val masterActorProps = springExtension.props("masterActor")
  
  val masterActor = 
    actorSystem.actorOf(masterActorProps, "MasterActor")
  val pingActor =
    actorSystem.actorOf(Props(classOf[PingActor],masterActor, appConfig), "PingActor")
  val pingMasterActor =
    actorSystem.actorOf(Props(classOf[PingMasterActor], masterActor, appConfig), "PingMasterActor")

  implicit val timeout: Timeout = appConfig.defaultTimeout

  def index = Action {
    Ok(views.html.index())
  }

  /**
   * retrieve an image by asking the MasterActor to route the request to any of the known
   * servers and return the result.
   */
  def image = CorsAction("*") {
    Action.async {
      (masterActor ? RequestImageId).mapTo[Response]
        .map(response =>
          response.status match {
            case 200 => Ok(success("Yeak"))
            case _ => BadRequest(response.body)
          })
        .recover {
        case conn : ConnectException => {
          ServiceUnavailable(error(conn.getMessage))
        }
        case connEx: Exception => {
            Logger.error(s"UNIDENTIFIED EXCEPTION : ${connEx.getMessage}")
            println(s"connex : ${connEx.toString} ${connEx.getMessage()}")
            println(connEx.getStackTraceString)
            ServiceUnavailable(error(connEx.getMessage))
          }
        }
    }
  }

  def saveTags = CorsAction("*") {
    Action.async(parse.json) {
      request =>
        Logger.info(s"""
          received image evaluation: ${request.body}
      
        """)
        val id = (request.body \ "id").asOpt[String]
        val tags = (request.body \ "tags").asOpt[List[String]]

        (id, tags) match {
          case (Some(id), Some(tags)) =>
            // TODO set explicit timeout and react to it
            // because than no ServerActor was reponsible for the image
            (masterActor ? Evaluation(id, tags)).mapTo[String]
              .map(r => Ok(r))

          case _ => Future { BadRequest(error(s"No image found for $id.")) }
        }
    }
  }

  /**
   * request new websocket channels from the [[backend.PingMasterActor]]
   */
  def ws = WebSocket.async[JsValue] {
    request =>
      // request new websocket channels from the PingMasterActor
      (pingMasterActor ? RequestWebSocket).mapTo[WebSocketResponse]
        .map { case WebSocketResponse(in, out) => (in, out) }
  }

  def jsRoutes(varName: String = "jsRoutes") = Action { implicit request =>
    Ok(Routes.javascriptRouter(varName)(
      routes.javascript.Application.image,
      routes.javascript.Application.saveTags))
      .as(JAVASCRIPT)
  }
}