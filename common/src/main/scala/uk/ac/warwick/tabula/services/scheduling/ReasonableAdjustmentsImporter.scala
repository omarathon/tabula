package uk.ac.warwick.tabula.services.scheduling

import java.time.format.DateTimeFormatter
import java.time.{OffsetDateTime, ZoneId}

import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.RequestBuilder
import org.springframework.stereotype.Service
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula.data.model.ReasonableAdjustment
import uk.ac.warwick.tabula.helpers.ExecutionContexts.global
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.services.scheduling.ReasonableAdjustmentsImporter._
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, AutowiringApacheHttpClientComponent, AutowiringTrustedApplicationsManagerComponent, TrustedApplicationsManagerComponent}

import scala.concurrent.Future
import scala.util.Try

object ReasonableAdjustmentsImporter {
  val iso8601DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSX").withZone(ZoneId.systemDefault())

  val readsOffsetDateTime: Reads[OffsetDateTime] = implicitly[Reads[String]].map(OffsetDateTime.parse(_, iso8601DateFormat))

  case class ReasonableAdjustments(
    universityID: String,
    reasonableAdjustments: Seq[ReasonableAdjustment],
    notes: String,
    lastUpdated: OffsetDateTime,
  )
  val readsReasonableAdjustments: Reads[ReasonableAdjustments] = (
    (__ \ "universityID").read[String] and
    (__ \ "reasonableAdjustments").read[Seq[ReasonableAdjustment]](Reads.seq(ReasonableAdjustment.formatsReasonableAdjustment)) and
    (__ \ "notes").read[String] and
    (__ \ "lastUpdated").read[OffsetDateTime](readsOffsetDateTime)
  )(ReasonableAdjustments.apply _)

  case class WellbeingCaseManagementConfiguration(
    baseUri: String,
    usercode: String,
  )
}

trait ReasonableAdjustmentsImporter {
  def getReasonableAdjustments(universityId: String): Future[Option[ReasonableAdjustments]]
}

trait ReasonableAdjustmentsImporterService extends ReasonableAdjustmentsImporter with Logging {
  self: WellbeingCaseManagementConfigurationComponent
    with ApacheHttpClientComponent
    with TrustedApplicationsManagerComponent =>

  override def getReasonableAdjustments(universityId: String): Future[Option[ReasonableAdjustments]] =
    Future {
      val req = RequestBuilder.get(s"${configuration.baseUri}/$universityId/reasonable-adjustments")
        .build()
      TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, configuration.usercode, req)

      val handler: ResponseHandler[Option[ReasonableAdjustments]] =
        ApacheHttpClientUtils.jsonResponseHandler { json =>
          json.validate[ReasonableAdjustments](readsReasonableAdjustments).fold(
            invalid => {
              logger.error(s"Error fetching reasonable adjustments for $universityId: $invalid")
              None
            },
            Option(_).filter(_.reasonableAdjustments.nonEmpty)
          )
        }

      Try(httpClient.execute(req, handler)).fold(
        t => {
          logger.error(s"Error fetching reasonable adjustments for $universityId", t)
          None
        },
        identity
      )
    }
}

// Intentionally not @Profile-scoped; the sandbox should still pull from wellbeing-sandbox
@Service("reasonableAdjustmentsImporterService")
class AutowiringReasonableAdjustmentsImporterService extends ReasonableAdjustmentsImporterService
  with AutowiringWellbeingCaseManagementConfigurationComponent
  with AutowiringApacheHttpClientComponent
  with AutowiringTrustedApplicationsManagerComponent

trait ReasonableAdjustmentsImporterComponent {
  def reasonableAdjustmentsImporter: ReasonableAdjustmentsImporter
}

trait AutowiringReasonableAdjustmentsImporterComponent extends ReasonableAdjustmentsImporterComponent {
  var reasonableAdjustmentsImporter: ReasonableAdjustmentsImporter = Wire[ReasonableAdjustmentsImporter]
}

trait WellbeingCaseManagementConfigurationComponent {
  def configuration: WellbeingCaseManagementConfiguration
}

trait AutowiringWellbeingCaseManagementConfigurationComponent extends WellbeingCaseManagementConfigurationComponent {
  lazy val configuration = WellbeingCaseManagementConfiguration(
    baseUri = Wire.property("${wellbeing.api.baseUri}"),
    usercode = Wire.property("${wellbeing.api.usercode}"),
  )
}