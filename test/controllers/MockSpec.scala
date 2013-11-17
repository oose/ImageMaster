package controllers

import scala.concurrent.Await
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent.duration.DurationInt
import org.specs2.runner._
import play.api.libs.ws.Response
import play.api.libs.json._
import play.api.test._
import _root_.util.ApplicationConfiguration
import org.junit.runner.RunWith
import akka.actor.ActorSystem
import org.specs2.mutable.After

@RunWith(classOf[JUnitRunner])
class MockSpec extends PlaySpecification  with After {

  implicit val atMost: Duration = 5 seconds

  val config = new ApplicationConfiguration {
    val serverNames = List("http://localhost:9001", "http://localhost:9002")
    val pingRepeat = 5 seconds
    val defaultTimeout = 5 seconds
    val actorSystem = ActorSystem()
    val service = new MockExternalRequestService {}
  }
  
  def after() = {
    config.actorSystem.shutdown()
    config.actorSystem.awaitTermination()
  }

  def await(f: Future[Response])(implicit atMost: Duration): Response = {
    Await.result(f,atMost)
  }

  "The mock object" should {
    "respond with ok on ping request" in {
      val result: scala.concurrent.Future[play.api.libs.ws.Response] = config.service.ping("asdfsadf")
      await(result).status must be equalTo (200)
    }
    "respond with ok on requestImage request" in {
      val result: scala.concurrent.Future[play.api.libs.ws.Response] = config.service.requestImage("asdfsadf")
      await(result).status must be equalTo (200)
    }
    "respond with ok on postImage request" in {
      val result: scala.concurrent.Future[play.api.libs.ws.Response] = config.service.postImage("asdfsadf", JsNull)
      await(result).status must be equalTo (200)
    }
  }
}