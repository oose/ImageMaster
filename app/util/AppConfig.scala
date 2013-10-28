package util

import scala.collection.JavaConversions._
import play.api._
import scala.concurrent.duration._

class AppConfig {
  
   val serverNames = {
      val value = Play.current.configuration.getStringList("imagemaster.serverlist")
      value match {
        case Some(names) => names.toList
        case None =>
          throw Play.current.configuration.globalError("Missing configuration key: [image.server]")
      } 
   }
   
   val defaultTimeout : FiniteDuration =  timeOutValue("imagemaster.defaulttimeout")
   
   val pingRepeat = timeOutValue("imagemaster.pingrepeat")
   
   private def timeOutValue(key: String) : FiniteDuration = {
      val value = Play.current.configuration.getMilliseconds(key)
      value match {
        case Some(duration) => FiniteDuration(duration, MILLISECONDS)
        case None =>
          throw Play.current.configuration.globalError(s"Missing configuration key: [$key]")
      } 
   }
}