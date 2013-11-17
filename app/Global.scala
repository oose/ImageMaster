import org.springframework.beans.factory.config.BeanDefinition
import org.springframework.context.ApplicationContext
import org.springframework.scala.context.function.FunctionalConfigApplicationContext
import org.springframework.scala.context.function.FunctionalConfiguration

import play.api._

import akka.actor._

import backend._

import oose.play.springextension.SpringExtensionImpl
import util.AppConfig

object Global extends GlobalSettings  {

  var ctx: FunctionalConfigApplicationContext = null

  override def onStart(app: Application) {
    super.onStart(app);
    ctx = FunctionalConfigApplicationContext(classOf[ImageMasterConfiguration])
  }

  override def onStop(app: Application) {
    Logger.info("""
          Stopping ImageMaster...
          
      """)
    ctx.stop()
    super.onStop(app)
  }

  override def getControllerInstance[A](controllerClass: Class[A]): A = {
    Logger.info(s"request controller instance for $controllerClass")
    ctx.getBean(controllerClass)
  }

}

class ImageMasterConfiguration extends FunctionalConfiguration {

  implicit val ctx = beanFactory.asInstanceOf[ApplicationContext]

  val actorSystem = singleton() {
    val system = ActorSystem("imagemaster")
    SpringExtensionImpl(system)
    system
  } destroy { as =>
    as.shutdown()
    as.awaitTermination()
  }

  val appConfig = singleton() {
    new AppConfig
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