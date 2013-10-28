import play.api._
import common.config.Configuration
import util.AppConfig

object Global extends GlobalSettings with Configuration {

  override def onStart(app: Application) {
    configure {
      Logger.info("""
          Starting ImageMaster...
          """)
      new AppConfig()
    }
  }
}