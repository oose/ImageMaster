package controllers

import play.api.libs.json.JsValue
import org.specs2.mock._
import _root_.util._

trait MockExternalRequestService extends ExternalRequestService with Mockito {

  def ping(url: String): scala.concurrent.Future[play.api.libs.ws.Response] = {
    val wsResponse = mock[play.api.libs.ws.Response]
    val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    wsResponse.status returns 200
    scala.concurrent.Future { wsResponse }(ec)
  }

  def postImage(url: String, json: JsValue): scala.concurrent.Future[play.api.libs.ws.Response] = {
    val wsResponse = mock[play.api.libs.ws.Response]
    val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    wsResponse.status returns 200
    scala.concurrent.Future { wsResponse }(ec)
  }

  def requestImage(url: String): scala.concurrent.Future[play.api.libs.ws.Response] = {
    val wsResponse = mock[play.api.libs.ws.Response]
    val ec = play.api.libs.concurrent.Execution.Implicits.defaultContext
    wsResponse.status returns 200
    scala.concurrent.Future { wsResponse }(ec)
  }
}