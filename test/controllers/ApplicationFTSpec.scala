package controllers

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.test.PlaySpecification
import play.api.test.WithBrowser
import play.api.test.FakeApplication
import play.api.test.WithApplication
import play.api.test.FakeRequest
import play.api.GlobalSettings
import oose.play.config.Configuration
import util.ApplicationConfiguration
import scala.concurrent.duration._

@RunWith(classOf[JUnitRunner])
class ApplicationFTSpec extends PlaySpecification {

  sequential

  val appWithGlobal = FakeApplication(withGlobal = Some(new GlobalSettings() with Configuration {
    override def onStart(app: play.api.Application) { 
      configure {
        new ApplicationConfiguration {
          val serverNames = List("http://localhost:9001", "http://localhost:9002")
          val pingRepeat = 5 seconds
          val defaultTimeout = 5 seconds
        }
      }
      
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
    "respond to the index Action" in new WithApplication(appWithGlobal) {
      val result = controllers.Application.index()(FakeRequest())

      status(result) must equalTo(OK)
      contentType(result) must beSome("text/html")
      contentAsString(result) must contain("Image Server Status")
    }

    "respond to the image Action" in new WithApplication(appWithGlobal) {
      val result = controllers.Application.image()(FakeRequest())
      
      status(result) must equalTo(SERVICE_UNAVAILABLE)
      contentType(result) must beSome("application/json")
    }
    
     "respond to the image route" in new WithApplication(appWithGlobal) {
      val Some(result) = route( FakeRequest("GET","/image"))
      
      status(result) must equalTo(SERVICE_UNAVAILABLE)
      contentType(result) must beSome("application/json")
    }
  }

}
