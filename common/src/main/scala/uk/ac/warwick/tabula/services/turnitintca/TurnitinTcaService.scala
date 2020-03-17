package uk.ac.warwick.tabula.services.turnitintca


import org.apache.http.HttpStatus
import org.apache.http.client.ResponseHandler
import org.apache.http.client.entity.EntityBuilder
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.entity.{ContentType, StringEntity}
import org.apache.http.util.EntityUtils
import org.joda.time.DateTime
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.util.web.Uri

import scala.concurrent.Future
import scala.util.Try

object TurnitinTcaService {

  /**
   * File formats and maximum filesize allowed by TCA listed at https://developers.turnitin.com/docs/tca/faq
   * Turnitin has requested we restrict file formats from being uploaded - see https://developers.turnitin.com/docs/tca/certification-review
   */
  val validExtensions: Seq[String] = Seq("pdf", "doc", "ppt", "pps", "xls", "docx", "pptx", "ppsx", "xlsx", "ps", "rtf", "htm", "html", "wpd", "odt", "txt")
  val maxFileSizeInMegabytes = 100
  val maxFileSize: Int = maxFileSizeInMegabytes * 1024 * 1024 // 100M

  def validFileType(file: FileAttachment): Boolean =
    validExtensions contains file.fileExt.toLowerCase

  def validFileSize(file: FileAttachment): Boolean =
    file.actualDataLength < maxFileSize

  def validFile(file: FileAttachment): Boolean = validFileType(file) && validFileSize(file)
}

case class TcaError(
  status: Int,
  message: String
)

object TcaError {
  val readsTcaError: Reads[TcaError] = (
    (__ \ "status").read[Int] and
    (__ \ "message").read[String]
  )(TcaError.apply _)
}

case class TurnitinTcaConfiguration(
  baseUri: String,
  integrationName: String,
  integrationKey: String,
  signingSecret: String
)

trait TurnitinTcaService {
  def createSubmission(fileAttachment: FileAttachment): Future[Either[String, TcaSubmission]]
  def getSubmissionInfo(fileAttachment: FileAttachment): Future[Either[String, TcaSubmission]]
  def getSimilarityReportInfo(fileAttachment: FileAttachment): Future[Either[String, TcaSimilarityReport]]
  def uploadSubmissionFile(fileAttachment: FileAttachment, tcaSubmission: TcaSubmission): Future[Either[String, TcaSubmission]]
  def requestSimilarityReport(tcaSubmission: TcaSubmission, fileAttachment: Option[FileAttachment] = None): Future[Unit]
  def saveSimilarityReportScores(tcaSimilarityReport: TcaSimilarityReport, fileAttachment: Option[FileAttachment] = None): Option[OriginalityReport]
  def persistMetadataToOriginalityReport(tcaSubmission: TcaSubmission, fileAttachment: Option[FileAttachment]): Option[OriginalityReport]
  def similarityReportUrl(originalityReport: OriginalityReport, user: CurrentUser): Future[Either[String, Uri]]
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

  // TODO check whether we will be bulk-approving the EULA on behalf of staff/students
  private def bulkEulaAcceptance: JsObject = Json.obj (
    "accepted_timestamp" -> "2019-09-01T00:00:00Z",
    "language" -> "en-US",
    "version"-> "v1beta"
  )

  override def createSubmission(fileAttachment: FileAttachment): Future[Either[String, TcaSubmission]] = {

    Future {
      val tabulaSubmission: Submission = fileAttachment.submissionValue.submission
      val assignment: Assignment = tabulaSubmission.assignment
      val module: Module = assignment.module
      val requestBody: JsObject = Json.obj(
        "owner" -> tabulaSubmission.studentIdentifier,
        "title" -> fileAttachment.name,
        "eula" -> bulkEulaAcceptance,
        "metadata" -> Json.obj(
          "group" -> Json.obj (
            "id" -> assignment.id,
            "name" -> assignment.name
          ),
          "group_context" -> Json.obj(
            "id" -> s"${module.id}-${assignment.academicYear}",
            "name"-> s"${module.name}-${assignment.academicYear}"
          )
        )
      )

      val req = tcaRequest(RequestBuilder.post(s"${tcaConfiguration.baseUri}/submissions"))
        .addHeader("Content-Type", s"application/json")
        .setEntity(new StringEntity(Json.stringify(requestBody), ContentType.APPLICATION_JSON))
        .build()

      val handler: ResponseHandler[Either[String, TcaSubmission]]  = ApacheHttpClientUtils.jsonResponseHandler { json =>

        val jsResult = json.validate[TcaSubmission](TcaSubmission.readsTcaSubmission).map(Right.apply).orElse(
          json.validate[TcaError](TcaError.readsTcaError).map(Left.apply)
        )

        jsResult.fold[Either[String, TcaSubmission]](
          invalid => {
            val message = s"Error parsing response when creating submission : $invalid\nResponse was: $json"
            logger.error(message)
            Left(message)
          },
          apiResponse => apiResponse.fold(
            error => Left(error.message),
            tcaSubmission =>
            fileAttachment.originalityReport match {
              case existingOriginalityReport =>
                saveTcaStatusToOriginalityReport(tcaSubmission, existingOriginalityReport)
                Right(tcaSubmission)
            }
          )
        )
      }

      // only try if it's not LTI, and create the Originality Report first if necessary
      fileAttachment.originalityReport match {
        case existingOriginalityReport if existingOriginalityReport != null && existingOriginalityReport.turnitinId != null =>
          val message = s"Not submitting or creating an originality report as one already exists for file: ${fileAttachment.id}"
          logger.warn(message)
          Left(message)
        case existingOriginalityReport => {
          // we haven't attempted TCA submission previously
          if (existingOriginalityReport == null) {
            logger.info(s"Creating blank Originality Report for ${fileAttachment.id}")
            val report = new OriginalityReport
            report.attachment = fileAttachment
            report.tcaSubmissionRequested = true
            fileAttachment.originalityReport = report
            report.lastSubmittedToTurnitin = DateTime.now()
            originalityReportService.saveOrUpdate(report)
          }
          Try(httpClient.execute(req, handler)).fold(
            t => {
              val message = s"Error requesting the creation of a submission for file - ${fileAttachment.id}, Tabula submission ${tabulaSubmission.id}, student ${tabulaSubmission.studentIdentifier}"
              logger.error(message, t)
              Left(message)
            },
            identity
          )
        }
      }
    }
  }

  override def getSubmissionInfo(fileAttachment: FileAttachment): Future[Either[String, TcaSubmission]] = {
    Future {

      require(fileAttachment.originalityReport.tcaSubmission != null)

      val req = tcaRequest(RequestBuilder.get(s"${tcaConfiguration.baseUri}/submissions/${fileAttachment.originalityReport.tcaSubmission}"))
        .addHeader("Content-Type", s"application/json")
        .build()

      val handler: ResponseHandler[Either[String, TcaSubmission]]  = ApacheHttpClientUtils.jsonResponseHandler { json =>

        val jsResult = json.validate[TcaSubmission](TcaSubmission.readsTcaSubmission).map(Right.apply).orElse(
          json.validate[TcaError](TcaError.readsTcaError).map(Left.apply)
        )

        jsResult.fold[Either[String, TcaSubmission]](
          invalid => {
            val message = s"Error parsing response when getting TCA submission info: $invalid\nResponse was: $json"
            logger.error(message)
            Left(message)
          },
          apiResponse => apiResponse.fold(
            error => Left(error.message),
            tcaSubmission => Right(tcaSubmission)
          )
        )
      }

      Try(httpClient.execute(req, handler)).fold(
        t => {
          val message = s"Error when attempting to get TCA submission info for: ${fileAttachment.id}"
          logger.error(message, t)
          Left(message)
        },
        identity
      )
    }
  }

  override def getSimilarityReportInfo(fileAttachment: FileAttachment): Future[Either[String, TcaSimilarityReport]] = Future {
    require(fileAttachment.originalityReport.tcaSubmission != null)

    val req = tcaRequest(RequestBuilder.get(s"${tcaConfiguration.baseUri}/submissions/${fileAttachment.originalityReport.tcaSubmission}/similarity"))
      .addHeader("Content-Type", s"application/json")
      .build()

    val handler: ResponseHandler[Either[String, TcaSimilarityReport]]  = ApacheHttpClientUtils.jsonResponseHandler { json =>

      val jsResult = json.validate[TcaSimilarityReport](TcaSimilarityReport.reads).map(Right.apply).orElse(
        json.validate[TcaError](TcaError.readsTcaError).map(Left.apply)
      )

      jsResult.fold[Either[String, TcaSimilarityReport]](
        invalid => {
          val message = s"Error parsing response when getting TCA similarity report info: $invalid\nResponse was: $json"
          logger.error(message)
          Left(message)
        },
        apiResponse => apiResponse.fold(
          error => Left(error.message),
          tcaSimilarityReport => Right(tcaSimilarityReport)
        )
      )
    }

    Try(httpClient.execute(req, handler)).fold(
      t => {
        val message = s"Error when attempting to get TCA similarity report info for: ${fileAttachment.originalityReport.tcaSubmission}"
        logger.error(message, t)
        Left(message)
      },
      identity
    )
  }

  override def uploadSubmissionFile(fileAttachment: FileAttachment, tcaSubmission: TcaSubmission): Future[Either[String, TcaSubmission]] = Future {
    require(fileAttachment.originalityReport.tcaSubmission == tcaSubmission.id)

    val req = tcaRequest(RequestBuilder.put(s"${tcaConfiguration.baseUri}/submissions/${tcaSubmission.id}/original"))
      .addHeader("Content-Type", s"binary/octet-stream")
      .addHeader("Content-Disposition", "inline;filename=\"" + fileAttachment.name + "\"")
      .setEntity(EntityBuilder.create().setStream(fileAttachment.asByteSource.openStream()).build())
      .build()

    val handler: ResponseHandler[Either[String, TcaSubmission]] = ApacheHttpClientUtils.handler {

      case response if response.getStatusLine.getStatusCode == HttpStatus.SC_ACCEPTED =>
        EntityUtils.consumeQuietly(response.getEntity)
        logger.info(s"successfully uploaded file to TCA submission: ${tcaSubmission.id}")
        Right(tcaSubmission)

      case response =>
        val message = s"Unexpected response when attempting to upload a file to TCA submission ${tcaSubmission.id}: $response"
        logger.error(message)
        Left(message)
    }

    Try(httpClient.execute(req, handler)).fold(
      t => {
        val message = s"Error when attempting to upload a file to TCA submission ${tcaSubmission.id}"
        logger.error(message, t)
        Left(message)
      },
      identity
    )
  }

  override def requestSimilarityReport(tcaSubmission: TcaSubmission, fileAttachment: Option[FileAttachment]): Future[Unit] = {

    val originalityReport = persistMetadataToOriginalityReport(tcaSubmission, fileAttachment)

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
      json.validate[Seq[TcaWebhook]](Reads.seq(TcaWebhook.reads)).recoverTotal(invalid => {
          logger.error(s"Error fetching webhooks - $invalid\nResponse $json")
          Seq()
      })
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

  override def saveSimilarityReportScores(tcaSimilarityReport: TcaSimilarityReport, fileAttachment: Option[FileAttachment]): Option[OriginalityReport] = {
    val originalityReport = fileAttachment.map(_.originalityReport).orElse(
      originalityReportService.getOriginalityReportByTcaSubmissionId(tcaSimilarityReport.submissionId)
    )

    // persist metadata
    originalityReport.foreach(or => {
      or.overlap = tcaSimilarityReport.overallMatch
      or.similarityRequestedOn = new DateTime(tcaSimilarityReport.requested.toInstant.toEpochMilli)
      or.similarityLastGenerated = new DateTime(tcaSimilarityReport.generated.toInstant.toEpochMilli)
      or.reportReceived = true
      originalityReportService.saveOrUpdate(or)
    })

    originalityReport
  }

  def saveTcaStatusToOriginalityReport(tcaSubmission: TcaSubmission, existingOriginalityReport: OriginalityReport): Unit = {
    existingOriginalityReport.lastSubmittedToTurnitin = DateTime.now()
    existingOriginalityReport.tcaSubmissionStatus = tcaSubmission.status
    existingOriginalityReport.tcaSubmission = tcaSubmission.id
    originalityReportService.saveOrUpdate(existingOriginalityReport)
  }

  def persistMetadataToOriginalityReport(tcaSubmission: TcaSubmission, fileAttachment: Option[FileAttachment]): Option[OriginalityReport] = {

    val originalityReport = fileAttachment.map(_.originalityReport).orElse(
      originalityReportService.getOriginalityReportByTcaSubmissionId(tcaSubmission.id)
    )

    originalityReport.foreach(or =>  {
      or.tcaSubmissionStatus = tcaSubmission.status
      or.errorCode = tcaSubmission.errorCode.orNull
      or.characterCount = tcaSubmission.characterCount
      or.pageCount = tcaSubmission.pageCount
      or.wordCount = tcaSubmission.wordCount
      originalityReportService.saveOrUpdate(or)

    })

    originalityReport
  }

  override def similarityReportUrl(originalityReport: OriginalityReport, user: CurrentUser): Future[Either[String, Uri]] = Future {
    val requestBody: JsObject = Json.obj(
      "viewer_user_id" -> user.apparentId,
      "locale" -> "en",
      "viewer_default_permission_set" -> "INSTRUCTOR"
    )

    val req = tcaRequest(RequestBuilder.post(s"${tcaConfiguration.baseUri}/submissions/${originalityReport.tcaSubmission}/viewer-url"))
      .addHeader("Content-Type", s"application/json")
      .setEntity(new StringEntity(Json.stringify(requestBody), ContentType.APPLICATION_JSON))
      .build()

    val uriRead: Reads[Uri] = (__ \ "viewer_url").read[String].map(Uri.parse)

    val handler: ResponseHandler[Either[String, Uri]] = ApacheHttpClientUtils.jsonResponseHandler { json =>

      val jsResult = json.validate[Uri](uriRead).map(Right.apply).orElse(
        json.validate[TcaError](TcaError.readsTcaError).map(e => Left(e.message))
      )

      jsResult.recoverTotal(invalid => {
        val message = s"Error fetching report url - $invalid"
        logger.error(s"$message\n$json")
        Left(message)
      })

    }

    Try(httpClient.execute(req, handler)).fold(
      t => {
        val message = s"Error requesting originality report for file ${originalityReport.attachment}"
        logger.error(message, t)
        Left(message)
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
