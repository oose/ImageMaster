import play.Project._

import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "angularjs" % "1.2.0-rc.3",
  "org.webjars" % "bootstrap" % "2.3.2"
)  

name := "ImageMaster"

version := "1.0-SNAPSHOT"

lazy val ImageCommon = RootProject(file("../ImageCommon/"))
 
play.Project.playScalaSettings

atmosPlaySettings

val ImageMaster = project.in(file("."))
  .aggregate(ImageCommon).dependsOn(ImageCommon)
