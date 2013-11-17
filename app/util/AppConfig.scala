package util

import scala.collection.JavaConversions._
import scala.concurrent.duration._
import play.api._
import play.api.libs.json.JsValue
import scala.concurrent.Future
import play.api.libs.ws.Response

trait ApplicationConfiguration {

  val service: ExternalRequestService
  
  val serverNames: List[String]
  
  val defaultTimeout: FiniteDuration
  
  val pingRepeat: FiniteDuration

}

trait ExternalRequestService {

  def requestImage(url: String): Future[Response]

  def postImage(url: String, json: JsValue): Future[Response]

  def ping(url: String): Future[Response]
}

class AppConfig extends ApplicationConfiguration {

  val service = new WebserviceRequestService {}

  lazy val serverNames: List[String] = {
    Play.current.configuration
      .getStringList("imagemaster.serverlist")
      .map(_.toList)
      .getOrElse {
        throw Play.current.configuration.globalError("Missing configuration key: [image.server]")
      }
  }

  lazy val defaultTimeout: FiniteDuration = timeOutValue("imagemaster.defaulttimeout")

  lazy val pingRepeat: FiniteDuration = timeOutValue("imagemaster.pingrepeat")

  private def timeOutValue(key: String): FiniteDuration = {
    Play.current.configuration.getMilliseconds(key)
      .map(d => FiniteDuration(d, MILLISECONDS))
      .getOrElse {
        throw Play.current.configuration.globalError(s"Missing configuration key: [$key]")
      }
  }
}

