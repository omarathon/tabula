package uk.ac.warwick.tabula.services.turnitintca

import org.apache.http.HttpStatus
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.OriginalityReport
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent, AutowiringOriginalityReportServiceComponent, AutowiringSubmissionServiceComponent, OriginalityReportServiceComponent, SubmissionServiceComponent}

import scala.concurrent.Future
import scala.util.Try
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.util.web.Uri


case class TurnitinTcaConfiguration(
  baseUri: String,
  integrationName: String,
  integrationKey: String,
  signingSecret: String
)

trait TurnitinTcaService {
  def requestSimilarityReport(tcaSubmission: TcaSubmission): Future[Unit]
  def saveSimilarityReportScores(tcaSimilarityReport: TcaSimilarityReport): Option[OriginalityReport]
  def similarityReportUrl(originalityReport: OriginalityReport, user: CurrentUser): Future[Option[Uri]]
  def listWebhooks: Future[Seq[TcaWebhook]]
  def registerWebhook(webhook: TcaWebhook): Future[Unit]
}

abstract class AbstractTurnitinTcaService extends TurnitinTcaService with Logging {
  self: SubmissionServiceComponent
    with OriginalityReportServiceComponent
    with ApacheHttpClientComponent
    with TurnitinTcaConfigurationComponent =>

  @Value("${build.time}") var buildTime: String = _

  private implicit val webhookWrites: Writes[TcaWebhook] = TcaWebhook.writes(tcaConfiguration.signingSecret)

  private def tcaRequest(value: RequestBuilder): RequestBuilder = value.addHeader("X-Turnitin-Integration-Name", tcaConfiguration.integrationName)
    .addHeader("X-Turnitin-Integration-Version", buildTime)
    .addHeader("Authorization", s"Bearer ${tcaConfiguration.integrationKey}")

  override def requestSimilarityReport(tcaSubmission: TcaSubmission): Future[Unit] = {
    val originalityReport = originalityReportService.getOriginalityReportByTcaSubmissionId(tcaSubmission.id)

    // persist metadata
    originalityReport.foreach(or => {
      or.tcaSubmissionStatus = tcaSubmission.status
      or.errorCode = tcaSubmission.errorCode.orNull
      or.characterCount = tcaSubmission.characterCount
      or.pageCount = tcaSubmission.pageCount
      or.wordCount = tcaSubmission.wordCount
      originalityReportService.saveOrUpdate(or)
    })

    (for (tca <- Option(tcaSubmission).filterNot(_.status == TcaSubmissionStatus.Error); or <- originalityReport) yield {
      Future {
        val submission = or.attachment.submissionValue.submission
        val assignment = submission.assignment

        val requestBody: JsObject = Json.obj(
          "indexing_settings" -> Json.obj(
            "add_to_index" -> assignment.turnitinStoreInRepository
          ),
          "generation_settings" -> Json.obj(
            "search_repositories" -> Json.arr(
              "INTERNET",
              "SUBMITTED_WORK",
              "PUBLICATION",
              "CROSSREF",
              "CROSSREF_POSTED_CONTENT"
            ),
            "auto_exclude_self_matching_scope" -> "ALL"
          ),
          "view_settings" -> Json.obj(
            "exclude_quotes" -> assignment.turnitinExcludeQuoted,
            "exclude_bibliography" -> assignment.turnitinExcludeBibliography
          )
        )

        val req = tcaRequest(RequestBuilder.put(s"${tcaConfiguration.baseUri}/submissions/${tca.id}/similarity"))
          .addHeader("Content-Type", s"application/json")
          .setEntity(new StringEntity(Json.stringify(requestBody), ContentType.APPLICATION_JSON))
          .build()

        val handler: ResponseHandler[Unit] = ApacheHttpClientUtils.handler {
          case response if response.getStatusLine.getStatusCode == HttpStatus.SC_ACCEPTED =>
            EntityUtils.consumeQuietly(response.getEntity)
            logger.info(s"Similarity Report requested for ${tca.id}")

          case response if response.getStatusLine.getStatusCode == HttpStatus.SC_CONFLICT =>
            EntityUtils.consumeQuietly(response.getEntity)
            logger.warn(s"A Similarity Report is already generating for ${tca.id}")

          case response =>
            logger.error(s"Unexpected response when requesting the generation of a similarity report: $response")
        }

        Try(httpClient.execute(req, handler)).fold(
          t => logger.error(s"Error requesting the generation of a similarity report for TCA ID - ${tca.id}, submission ${submission.id}, student ${submission.studentIdentifier}", t),
          identity
        )
      }
    }).getOrElse(Future.unit)
  }

  override def listWebhooks: Future[Seq[TcaWebhook]] = Future {
    val req = tcaRequest(RequestBuilder.get(s"${tcaConfiguration.baseUri}/webhooks")).build()

    val handler: ResponseHandler[Seq[TcaWebhook]] = ApacheHttpClientUtils.jsonResponseHandler { json =>
      json.validate[Seq[TcaWebhook]](Reads.seq(TcaWebhook.reads)).fold(
        invalid => {
          logger.error(s"Error fetching webhooks - $invalid")
          Seq()
        },
        identity
      )
    }

    Try(httpClient.execute(req, handler)).fold(
      t => {
        logger.error(s"Error fetching webhooks", t)
        Seq()
      },
      identity
    )
  }

  override def registerWebhook(webhook: TcaWebhook): Future[Unit] = Future {
    logger.info(s"Registering ${webhook.description} webhook")
    val requestBody: JsValue = Json.toJson(webhook)

    val req = tcaRequest(RequestBuilder.post(s"${tcaConfiguration.baseUri}/webhooks"))
      .addHeader("Content-Type", s"application/json")
      .setEntity(new StringEntity(Json.stringify(requestBody), ContentType.APPLICATION_JSON))
      .build()

    val handler: ResponseHandler[Unit] = ApacheHttpClientUtils.handler {

      case response if response.getStatusLine.getStatusCode == HttpStatus.SC_CREATED =>
        EntityUtils.consumeQuietly(response.getEntity)
        logger.info(s"${webhook.description} webhook registered")

      case response =>
        logger.error(s"Unexpected response when registering webhook ${webhook.description}: $response")
    }

    Try(httpClient.execute(req, handler)).fold(t => logger.error(s"Error when registering webhook ${webhook.description}", t), identity)
  }

  override def saveSimilarityReportScores(tcaSimilarityReport: TcaSimilarityReport): Option[OriginalityReport] = {
    val originalityReport = originalityReportService.getOriginalityReportByTcaSubmissionId(tcaSimilarityReport.submissionId)

    // persist metadata
    originalityReport.foreach(or => {
      or.matchPercentage = tcaSimilarityReport.overallMatch
      or.similarityRequestedOn = new DateTime(tcaSimilarityReport.requested.toInstant.toEpochMilli)
      or.similarityLastGenerated = new DateTime(tcaSimilarityReport.generated.toInstant.toEpochMilli)
      originalityReportService.saveOrUpdate(or)
    })

    originalityReport
  }

  override def similarityReportUrl(originalityReport: OriginalityReport, user: CurrentUser): Future[Option[Uri]] = Future {
    val requestBody: JsObject = Json.obj(
      "viewer_user_id" -> user.apparentId,
      "locale" -> "en",
      "viewer_default_permission_set" -> "INSTRUCTOR"
    )

    val req = tcaRequest(RequestBuilder.put(s"${tcaConfiguration.baseUri}/submissions/${originalityReport.tcaSubmission}/viewer-url"))
      .addHeader("Content-Type", s"application/json")
      .setEntity(new StringEntity(Json.stringify(requestBody), ContentType.APPLICATION_JSON))
      .build()

    val uriRead: Reads[Uri] = (__ \ "viewer_url").read[String].map(Uri.parse)

    val handler: ResponseHandler[Option[Uri]] = ApacheHttpClientUtils.jsonResponseHandler { json =>
      json.validate[Uri](uriRead).fold(
        invalid => {
          logger.error(s"Error fetching report url - $invalid")
          None
        },
        uri => Some(uri)
      )
    }

    Try(httpClient.execute(req, handler)).fold(
      t => {
        logger.error(s"Error requesting originality report for file ${originalityReport.attachment}", t)
        None
      },
      identity
    )
  }
}

@Service("turnitinTcaService")
class AutowiringTurnitinTcaService
  extends AbstractTurnitinTcaService
    with AutowiringSubmissionServiceComponent
    with AutowiringOriginalityReportServiceComponent
    with AutowiringApacheHttpClientComponent
    with AutowiringTurnitinTcaConfigurationComponent

trait TurnitinTcaServiceComponent {
  def turnitinTcaService: TurnitinTcaService
}

trait AutowiringTurnitinTcaServiceComponent extends TurnitinTcaServiceComponent {
  var turnitinTcaService: TurnitinTcaService = Wire[TurnitinTcaService]
}

trait TurnitinTcaConfigurationComponent {
  def tcaConfiguration: TurnitinTcaConfiguration
}

trait AutowiringTurnitinTcaConfigurationComponent extends TurnitinTcaConfigurationComponent {
  lazy val tcaConfiguration: TurnitinTcaConfiguration = TurnitinTcaConfiguration(
    baseUri = Wire.property("${turnitin.tca.baseUri}"),
    integrationName = Wire.property("${turnitin.tca.integrationName}"),
    integrationKey = Wire.property("${turnitin.tca.integrationKey}"),
    signingSecret = Wire.property("${turnitin.tca.signingSecret}")
  )
}