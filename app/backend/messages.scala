package backend

import scala.concurrent.duration._

import play.api.libs.iteratee.Enumerator
import play.api.libs.iteratee.Iteratee
import play.api.libs.json.JsValue

case object RequestImageId
case class Evaluation(id: String, tags: List[String])
case object Ping

sealed trait PingResponse
case class Pong(id: String) extends PingResponse
case class TimeoutPong(id: String, duration : Option[FiniteDuration] = None) extends PingResponse

trait EvaluationStatus
case object EvaluationAccepted extends EvaluationStatus
case class EvaluationRejected(reason: String) extends EvaluationStatus


case object RequestWebSocket
case object Quit
case class WebSocketResponse(in: Iteratee[JsValue,_], out: Enumerator[JsValue])

