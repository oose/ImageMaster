package backend

import play.api.libs.json._

trait LowlevelImplicits {
  implicit val pingResponseWrite: Writes[PingResponse] = new Writes[PingResponse] {
    override def writes(pr: PingResponse) = {
      pr match {
        case Pong(id) =>
          Json.toJson(Map("id" -> id, "state" -> "pong"))
        case TimeoutPong(id, None) => Json.toJson(Map("id" -> id, "state" -> "timeout"))
        case TimeoutPong(id, Some(duration)) => Json.toJson(Map("id" -> id, "state" -> "timeout", "remainingDuration" -> duration.toString))
      }
    }
  }
  
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

object Implicits extends LowlevelImplicits