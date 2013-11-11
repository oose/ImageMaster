package controllers

import org.junit.runner.RunWith
import org.specs2.mutable._
import org.specs2.runner.JUnitRunner
import play.api.test.PlaySpecification
import play.api.test.WithBrowser
import play.api.test.FakeApplication

@RunWith(classOf[JUnitRunner])
class ApplicationFTSpec extends PlaySpecification {

  "The application" should {
    "start in the browser" in new WithBrowser(play.api.test.Helpers.FIREFOX) {

      browser.goTo("/")

      browser.url must equalTo("/")
      browser.pageSource must contain("Image Server Status")
      browser.title must equalTo("Master")
    }

  }

}
