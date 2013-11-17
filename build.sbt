import com.typesafe.sbt.SbtAtmosPlay.atmosPlaySettings

import play.Project._

resolvers += "oose (snapshots)" at "http://oose.github.io/m2/snapshots"

libraryDependencies ++= Seq(
  "org.webjars" %% "webjars-play" % "2.2.0",
  "org.webjars" % "angularjs" % "1.2.0-rc.3",
  "org.webjars" % "bootstrap" % "2.3.2",
  "oose.play" %% "config" % "1.0-SNAPSHOT",
  "oose.play" %% "actions" % "1.0-SNAPSHOT",
  "org.specs2" % "classycle" % "1.4.1" % "test",
  "org.mockito" % "mockito-all" % "1.9.5" % "test"
)  

name := "ImageMaster"

version := "1.0-SNAPSHOT"
 
playScalaSettings

atmosPlaySettings

val ImageMaster = project.in(file("."))
