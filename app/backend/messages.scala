package backend

import play.api.libs.json._
import play.api.libs.functional.syntax._
import play.api.libs.iteratee.Iteratee
import play.api.libs.iteratee.Enumerator

case object RequestId
case class Evaluation(id: String, tags: List[String])
case object Ping

sealed trait PingResponse
case class Pong(id: String) extends PingResponse
case class TimeoutPong(id: String) extends PingResponse

trait EvaluationStatus
case object EvaluationAccepted extends EvaluationStatus
case class EvaluationRejected(reason: String) extends EvaluationStatus

object PingResponse {
  implicit val pingResponseWrite: Writes[PingResponse] = new Writes[PingResponse] {
    override def writes(pr: PingResponse) = {
      pr match {
        case Pong(id) =>
          Json.toJson(Map("id" -> id, "state" -> "pong"))
        case TimeoutPong(id) => Json.toJson(Map("id" -> id, "state" -> "timeout"))
      }
    }
  }
}

case object RequestWebSocket
case object Quit
case class WebSocketResponse(in: Iteratee[JsValue,_], out: Enumerator[JsValue])

object Evaluation {
  implicit val formats: Format[Evaluation] = new Format[Evaluation] {

    override def writes(e: Evaluation) = {
      Json.toJson(Map("id" -> Json.toJson(e.id), "tags" -> Json.toJson(e.tags)))
    }

    override def reads(js: JsValue) = {
      JsSuccess(
        Evaluation((js \ "id").as[String],
          (js \ "tags").as[List[String]]))
    }

  }
}