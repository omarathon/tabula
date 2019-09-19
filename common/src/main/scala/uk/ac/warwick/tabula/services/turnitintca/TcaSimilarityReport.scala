package uk.ac.warwick.tabula.services.turnitintca

import java.time.{OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter

import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import enumeratum.EnumEntry.Uppercase
import enumeratum.{Enum, EnumEntry}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.EnumTwoWayConverter

import scala.collection.immutable


sealed abstract class TcaSimilarityStatus extends EnumEntry with Uppercase
object TcaSimilarityStatus extends Enum[TcaSimilarityStatus] {
  case object Processing extends TcaSimilarityStatus
  case object Complete extends TcaSimilarityStatus

  override val values: immutable.IndexedSeq[TcaSimilarityStatus] = findValues
  val reads: Reads[TcaSimilarityStatus] = implicitly[Reads[String]].map(TcaSimilarityStatus.withName)
}

class TcaSimilarityStatusUserType extends EnumUserType(TcaSubmissionStatus)
class TcaSimilarityStatusConverter extends EnumTwoWayConverter(TcaSubmissionStatus)

case class TcaSourceMatch(
  name: String,
  percentage: Double
)

object TcaSourceMatch {
  val reads: Reads[TcaSourceMatch] = (
    (__ \ "name").read[String] and
    (__ \ "percentage").read[Double]
  )(TcaSourceMatch.apply _)
}

class TcaSimilarityReportDeserializer extends JsonDeserializer[TcaSimilarityReport] with Logging {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): TcaSimilarityReport = {
    val node: JsonNode = jsonParser.getCodec.readTree(jsonParser)
    val mapper = (new ObjectMapper).registerModule(new PlayJsonModule(JsonParserSettings.settings))
    val json = mapper.treeToValue(node, classOf[JsValue])

    json.validate[TcaSimilarityReport](TcaSimilarityReport.reads).fold(
      invalid => {
        logger.error(s"Error parsing similarity complete request: $invalid")
        logger.error(s"Original request was: $json")
        null
      },
      s => s
    )
  }
}

@JsonDeserialize(using = classOf[TcaSimilarityReportDeserializer])
case class TcaSimilarityReport (
  submissionId: String,
  overallMatch: Option[Int],
  status: TcaSimilarityStatus,
  requested: OffsetDateTime,
  generated: OffsetDateTime,
  topSourceLargestMatchWords: Option[Int],
  topMatches: Seq[TcaSourceMatch]
)

object TcaSimilarityReport {
  val iso8601DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.systemDefault())

  val readsOffsetDateTime: Reads[OffsetDateTime] = implicitly[Reads[String]].map(OffsetDateTime.parse(_, iso8601DateFormat))

  val reads: Reads[TcaSimilarityReport] = (
    (__ \ "submission_id").read[String] and
    (__ \ "overall_match_percentage").readNullable[Int] and
    (__ \ "status").read[TcaSimilarityStatus](TcaSimilarityStatus.reads) and
    (__ \ "time_requested").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "time_generated").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "top_source_largest_matched_word_count").readNullable[Int] and
    (__ \ "top_matches" ).read[Seq[TcaSourceMatch]](Reads.seq(TcaSourceMatch.reads))
  )(TcaSimilarityReport.apply _)
}
