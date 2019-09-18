package uk.ac.warwick.tabula.services.turnitintca


import java.time.{OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import enumeratum.{Enum, EnumEntry}
import enumeratum.EnumEntry.UpperSnakecase
import play.api.libs.functional.syntax._
import play.api.libs.json._

import scala.collection.immutable


sealed abstract class TcaEventType extends EnumEntry with UpperSnakecase
object TcaEventType extends Enum[TcaEventType] {
  case object SubmissionComplete extends TcaEventType
  case object SimilarityComplete extends TcaEventType
  case object SimilarityUpdated extends TcaEventType
  case object PdfStatus extends TcaEventType

  override val values: immutable.IndexedSeq[TcaEventType] = findValues
  val reads: Reads[TcaEventType] = implicitly[Reads[String]].map(TcaEventType.withName)
  val writes: Writes[TcaEventType] = implicitly[Writes[String]].contramap(_.entryName)
}

case class TcaWebhook(
  id: String,
  url: String,
  description: String,
  createdDate: OffsetDateTime,
  eventTypes: Seq[TcaEventType]
)

object TcaWebhook {

  def apply(url: String, description: String, eventTypes: Seq[TcaEventType]): TcaWebhook = TcaWebhook(null, url, description, null, eventTypes)

  final val SubmissionWebhook: String = "Tabula.SubmissionComplete"
  final val SimilarityWebhook: String = "Tabula.SimilarityComplete"

  val iso8601DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.systemDefault())

  val readsOffsetDateTime: Reads[OffsetDateTime] = implicitly[Reads[String]].map(OffsetDateTime.parse(_, iso8601DateFormat))

  val reads: Reads[TcaWebhook] = (
    (__ \ "id").read[String] and
    (__ \ "url").read[String] and
    (__ \ "description").read[String] and
    (__ \ "created_time").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "event_types").read[Seq[TcaEventType]](Reads.seq(TcaEventType.reads))
  )(TcaWebhook.apply( _: String, _:String, _:String, _:OffsetDateTime, _:Seq[TcaEventType]))

  implicit val eventTypeWrites: Writes[Seq[TcaEventType]] = Writes.seq(TcaEventType.writes)

  def writes(signingSecret: String): Writes[TcaWebhook] = w => Json.obj(
    "signing_secret" -> signingSecret,
    "url" -> w.url,
    "description" -> w.description,
    "event_types" -> w.eventTypes
  )
}
