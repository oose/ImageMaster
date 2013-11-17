package design

import org.specs2.mutable._
import org.specs2.specification.Analysis

class DependencySpec extends SpecificationWithJUnit with Analysis  {

  val design = layers(
    "views",
    "controllers",
    "backend",
    "util"
    ).inTargetDir("target/scala-2.10/classes")

  "Program design" should {
    "adhere to layer structure" in {
    	design must beRespected
    }
  }
}