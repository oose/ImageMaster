package controllers

import scala.concurrent._
import scala.concurrent.duration._

import org.junit.runner.RunWith
import org.specs2.mock._
import org.specs2.runner.JUnitRunner
import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext
import org.springframework.scala.context.function.FunctionalConfiguration

import play.api._
import play.api.test._

import akka.actor._

import backend.MasterActor
import oose.play.config.Configuration
import oose.play.springextension.SpringExtensionImpl
import _root_.util.ApplicationConfiguration

@RunWith(classOf[JUnitRunner])
class ApplicationFTSpec extends PlaySpecification with Mockito {

  sequential

  def appWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() {
    var ctx: FunctionalConfigApplicationContext = _
    override def onStart(app: play.api.Application) {
      println(s"In fake global ${app == null}")
//      super.onStart(app)
      ctx = FunctionalConfigApplicationContext(classOf[TestConfiguration])
    }

    override def onStop(app: play.api.Application) {
 //     super.onStop(app)
      ctx.stop()
    }

    override def getControllerInstance[A](controllerClass: Class[A]): A = {
      Logger.info(s"request controller instance for $controllerClass")
      ctx.getBean(controllerClass)
    }
  }))

  "The application" should {
    "start in the browser" in new WithBrowser(webDriver = FIREFOX, app = appWithGlobal) {

      browser.goTo("/")
      browser.url must equalTo("/")
      browser.pageSource must contain("Image Server Status")
      browser.title must equalTo("Master")
    }
  }

  "The application controller" should {
    "respond to the index route" in new WithApplication(appWithGlobal) {

      val Some(result) = route(FakeRequest("GET", "/"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      contentAsString(result) must contain("Image Server Status")
    }

    "respond to the image route" in new WithApplication(appWithGlobal) {

      val Some(result) = route(FakeRequest("GET", "/image"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
    }

    "respond to the image route" in new WithApplication(appWithGlobal) {
      val Some(result) = route(FakeRequest("GET", "/image"))

      status(result) must equalTo(OK)
      contentType(result) must beSome("application/json")
    }
  }

}

class TestConfiguration extends FunctionalConfiguration {

  implicit val ctx = beanFactory.asInstanceOf[ApplicationContext]

  val actorSystem = singleton() {
    val system = ActorSystem("TestApplication")
    SpringExtensionImpl(system)
    system
  } destroy { as =>
    as.shutdown()
    as.awaitTermination()
  }

  val appConfig = singleton() {
    val ac : ApplicationConfiguration = new ApplicationConfiguration  {
      val service = new MockExternalRequestService {}
      val serverNames = List("http://localhost:9001", "http://localhost:9002")
      val pingRepeat = 5 seconds
      val defaultTimeout = 5 seconds
    }
    ac
  }

  val appCtrl = singleton() {
    val app = new controllers.Application(appConfig())(actorSystem(), ctx)
    app
  }

  val masterActor = bean("masterActor", scope = BeanDefinition.SCOPE_PROTOTYPE) {
    val masterActor = new MasterActor(appConfig())
    masterActor
  }
}
