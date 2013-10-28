import sbt._
import Keys._
import play.Project._
import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

object ApplicationBuild extends Build {

  val appName         = "ImageMaster"
  val appVersion      = "1.0-SNAPSHOT"

  val appDependencies = Seq(
    "org.webjars" %% "webjars-play" % "2.2.0",
    "org.webjars" % "angularjs" % "1.2.0rc1",
    "org.webjars" % "bootstrap" % "2.3.2"
  )
  
  lazy val imageCommon = RootProject(file("../ImageCommon/"))

  val main = play.Project(appName, appVersion, appDependencies).settings(
    resolvers += "Spring Scala" at "http://repo.springsource.org/milestone/"     
  )
  .settings(atmosPlaySettings: _*)
  .aggregate(imageCommon).dependsOn(imageCommon)

}
