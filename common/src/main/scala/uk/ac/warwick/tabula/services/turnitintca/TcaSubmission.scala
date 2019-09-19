package uk.ac.warwick.tabula.services.turnitintca


import java.time.{OffsetDateTime, ZoneId}
import java.time.format.DateTimeFormatter
import com.fasterxml.jackson.core.JsonParser
import com.fasterxml.jackson.databind.{DeserializationContext, JsonDeserializer, JsonNode, ObjectMapper}
import com.fasterxml.jackson.databind.annotation.JsonDeserialize
import enumeratum.EnumEntry.{UpperSnakecase, Uppercase}
import enumeratum.{Enum, EnumEntry}
import play.api.libs.functional.syntax._
import play.api.libs.json._
import play.api.libs.json.jackson.PlayJsonModule
import uk.ac.warwick.tabula.data.model.EnumUserType
import uk.ac.warwick.tabula.helpers.Logging
import scala.collection.immutable
import scala.util.{Success, Try}

import uk.ac.warwick.tabula.system.EnumTwoWayConverter


sealed abstract class TcaSubmissionStatus extends EnumEntry with Uppercase
object TcaSubmissionStatus extends Enum[TcaSubmissionStatus] {
  case object Created extends TcaSubmissionStatus
  case object Processing extends TcaSubmissionStatus
  case object Complete extends TcaSubmissionStatus
  case object Error extends TcaSubmissionStatus

  override val values: immutable.IndexedSeq[TcaSubmissionStatus] = findValues
  val reads: Reads[TcaSubmissionStatus] = implicitly[Reads[String]].map(TcaSubmissionStatus.withName)
}

class TcaSubmissionStatusUserType extends EnumUserType(TcaSubmissionStatus)
class TcaSubmissionStatusConverter extends EnumTwoWayConverter(TcaSubmissionStatus)

sealed abstract class TcaErrorCode(val description: String) extends EnumEntry with UpperSnakecase
object TcaErrorCode extends Enum[TcaErrorCode] {
  case object UnsupportedFiletype extends TcaErrorCode("The uploaded filetype is not supported")
  case object ProcessingError extends TcaErrorCode("An unspecified error occurred while processing the submissions")
  case object TooLittleText extends TcaErrorCode("The submission does not have enough text to generate a Similarity Report (a submission must contain at least 20 words)")
  case object TooMuchText extends TcaErrorCode("The submission has too much text to generate a Similarity Report (after extracted text is converted, the submission must contain less than 2MB of text)")
  case object CannotExtractText extends TcaErrorCode("The submission does not contain any text")
  case object TooManyPages extends TcaErrorCode("The submission has too many pages to generate a Similarity Report (a submission cannot contain more than 800 pages)")
  case object FileLocked extends TcaErrorCode("The uploaded file requires a password in order to be opened")
  case object CorruptFile extends TcaErrorCode("The uploaded file appears to be corrupt")

  override val values: immutable.IndexedSeq[TcaErrorCode] = findValues
  val reads: Reads[TcaErrorCode] = implicitly[Reads[String]].map(TcaErrorCode.withName)
}

class TcaErrorCodeUserType extends EnumUserType(TcaErrorCode)
class TcaErrorCodeConverter extends EnumTwoWayConverter(TcaErrorCode)

case class CustomMetadata(
  fileAttachmentId: String,
  submissionId: String
)

object CustomMetadata {
  val readCustomMetadata: Reads[CustomMetadata] = implicitly[Reads[String]]
    .map(s => Try(Json.parse(s)))
    .collect(JsonValidationError("Could not parse custom metadata")) {
      case Success(js) => js
    }
    .andThen((
      (__ \ "file_attachment_id").read[String] and
      (__ \ "submission_id").read[String]
    )(CustomMetadata.apply _))
}



class TcaSubmissionDeserializer extends JsonDeserializer[TcaSubmission] with Logging {
  override def deserialize(jsonParser: JsonParser, ctxt: DeserializationContext): TcaSubmission = {

    val node: JsonNode = jsonParser.getCodec.readTree(jsonParser)
    val mapper = (new ObjectMapper).registerModule(new PlayJsonModule(JsonParserSettings.settings))
    val json = mapper.treeToValue(node, classOf[JsValue])

    json.validate[TcaSubmission](TcaSubmission.readsTcaSubmission).fold(
      invalid => {
        logger.error(s"Error parsing submission complete request: $invalid")
        logger.error(s"Original request was: $json")
        null
      },
      s => s
    )
  }
}

@JsonDeserialize(using = classOf[TcaSubmissionDeserializer])
case class TcaSubmission(
  id: String,
  student: String,
  created: OffsetDateTime,
  status: TcaSubmissionStatus,
  pageCount: Option[Int],
  wordCount: Option[Int],
  characterCount: Option[Int],
  contentType: Option[String],
  errorCode: Option[TcaErrorCode],
  metadata: Option[CustomMetadata]
)

object TcaSubmission {
  val iso8601DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.systemDefault())

  val readsOffsetDateTime: Reads[OffsetDateTime] = implicitly[Reads[String]].map(OffsetDateTime.parse(_, iso8601DateFormat))

  val readsTcaSubmission: Reads[TcaSubmission] = (
    (__ \ "id").read[String] and
    (__ \ "owner").read[String] and
    (__ \ "created_time").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "status").read[TcaSubmissionStatus](TcaSubmissionStatus.reads) and
    (__ \ "page_count").readNullable[Int] and
    (__ \ "word_count").readNullable[Int] and
    (__ \ "character_count").readNullable[Int] and
    (__ \ "content_type").readNullable[String] and
    (__ \ "error_code").readNullable[TcaErrorCode](TcaErrorCode.reads) and
    (__ \ "metadata" \ "custom").readNullable[CustomMetadata](CustomMetadata.readCustomMetadata)
  )(TcaSubmission.apply _)
}
