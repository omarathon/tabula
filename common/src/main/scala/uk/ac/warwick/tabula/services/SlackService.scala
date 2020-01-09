package uk.ac.warwick.tabula.services

import enumeratum.{Enum, EnumEntry, PlayJsonEnum}
import org.apache.http.client.HttpResponseException
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.ContentType
import org.apache.http.util.EntityUtils
import org.apache.http.{HttpStatus, StatusLine}
import org.springframework.stereotype.Service
import play.api.libs.json.{Json, JsonConfiguration, JsonNaming, Writes}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.services.SlackService.MessagePayload

import scala.concurrent.Future

object SlackService {
  sealed abstract class TextFormattingType(override val entryName: String) extends EnumEntry
  object TextFormattingType extends Enum[TextFormattingType] with PlayJsonEnum[TextFormattingType] {
    case object PlainText extends TextFormattingType("plain_text")
    case object Markdown extends TextFormattingType("mrkdwn")

    override def values: IndexedSeq[TextFormattingType] = findValues
  }

  case class Text private (
    `type`: TextFormattingType,
    text: String,
    emoji: Option[Boolean] = None, // For TextFormattingType.PlainText, should emojis be escaped into the colon emoji format?
    verbatim: Option[Boolean] = None // For TextFormattingType.Markdown, set to true to skip pre-processing of auto-converted URLs, mentions etc
  )

  object Text {
    def plain(text: String, emoji: Boolean = false): Text = Text(`type` = TextFormattingType.PlainText, text = text, emoji = Some(emoji))
    def markdown(text: String, verbatim: Boolean = false): Text = Text(`type` = TextFormattingType.Markdown, text = text, verbatim = Some(verbatim))
  }

  sealed abstract class LayoutBlock(val `type`: String)
  case class SectionBlock private (
    text: Text, // Text for the block
    block_id: Option[String] = None,
    fields: Option[Seq[Text]],
    // accessory not implemented
  ) extends LayoutBlock("section")

  object SectionBlock {
    def apply(text: Text): SectionBlock = SectionBlock(text = text, fields = None)
    def apply(text: Text, fields: Seq[Text]): SectionBlock = SectionBlock(text = text, fields = Some(fields))
  }

  case class DividerBlock private (
    block_id: Option[String] = None,
  ) extends LayoutBlock("divider")

  object DividerBlock {
    def apply(): DividerBlock = DividerBlock(block_id = None)
  }

  case class MessagePayload private (
    text: String, // Body of the message (or fallback, if blocks is non-empty)
    blocks: Option[Seq[LayoutBlock]],
    thread_ts: Option[String] = None, // thread to reply to
    mrkdwn: Boolean, // whether text is rendered with markdown or not
  )

  object MessagePayload {
    def simple(text: String, markdown: Boolean = true): MessagePayload =
      MessagePayload(text = text, mrkdwn = markdown, blocks = None)

    def blocks(blocks: Seq[LayoutBlock], fallbackText: String, fallbackTextMarkdown: Boolean = true): MessagePayload =
      MessagePayload(blocks = Some(blocks), text = fallbackText, mrkdwn = fallbackTextMarkdown)
  }

  implicit val jsonConfiguration: JsonConfiguration = JsonConfiguration(
    discriminator = "type",
    typeNaming = JsonNaming {
      case "uk.ac.warwick.tabula.services.SlackService.SectionBlock" => "section"
      case "uk.ac.warwick.tabula.services.SlackService.DividerBlock" => "divider"
      case o => o
    }
  )

  implicit val writesText: Writes[Text] = Json.writes[Text]
  implicit val writesSectionBlock: Writes[SectionBlock] = Json.writes[SectionBlock]
  implicit val writesDividerBlock: Writes[DividerBlock] = Json.writes[DividerBlock]
  implicit val writesLayoutBlock: Writes[LayoutBlock] = Json.writes[LayoutBlock]
  implicit val writesMessagePayload: Writes[MessagePayload] = Json.writes[MessagePayload]
}

trait SlackService {
  def postMessage(webhookUrl: String, message: MessagePayload): Future[Unit]
}

abstract class AbstractSlackService extends SlackService with Logging {
  self: ApacheHttpClientComponent =>

  override def postMessage(webhookUrl: String, message: MessagePayload): Future[Unit] = Future {
    val request =
      RequestBuilder.post(webhookUrl)
        .setEntity(
          EntityBuilder.create()
            .setContentType(ContentType.APPLICATION_JSON)
            .setText(Json.stringify(Json.toJson(message)))
            .build()
        )
        .build()

    httpClient.execute(
      request,
      ApacheHttpClientUtils.handler {
        // Everything is fine
        case response if response.getStatusLine.getStatusCode == HttpStatus.SC_OK =>
          EntityUtils.consumeQuietly(response.getEntity)

        // Everything is not fine and it's Tabula's fault
        case response if response.getStatusLine.getStatusCode == HttpStatus.SC_BAD_REQUEST =>
          val statusLine: StatusLine = response.getStatusLine
          val body = EntityUtils.toString(response.getEntity)

          logger.error(s"There was an error ($body) posting a message to Slack. Request:\n\n${Json.prettyPrint(Json.toJson(message))}")

          throw new HttpResponseException(statusLine.getStatusCode, statusLine.getReasonPhrase)
      }
    )
  }
}

@Service("slackService")
class AutowiredSlackService extends AbstractSlackService
  with AutowiringApacheHttpClientComponent

trait SlackServiceComponent {
  def slackService: SlackService
}

trait AutowiringSlackServiceComponent extends SlackServiceComponent {
  var slackService: SlackService = Wire[SlackService]
}
