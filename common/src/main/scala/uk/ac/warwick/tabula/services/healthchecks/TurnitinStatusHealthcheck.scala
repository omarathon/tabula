package uk.ac.warwick.tabula.services.healthchecks

import java.time.format.DateTimeFormatter
import java.time.{LocalDateTime, OffsetDateTime, ZoneId}

import enumeratum.{Enum, EnumEntry, EnumFormats, PlayJsonEnum}
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.RequestBuilder
import org.springframework.context.annotation.Profile
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component
import play.api.libs.functional.syntax._
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.services.AutowiringApacheHttpClientComponent
import uk.ac.warwick.tabula.services.healthchecks.TurnitinStatusHealthcheck._
import uk.ac.warwick.util.core.DateTimeUtils
import uk.ac.warwick.util.service.{ServiceHealthcheck, ServiceHealthcheckProvider}

import scala.util.Try

object TurnitinStatusHealthcheck {
  val Name = "turnitin-status"
  val InitialState = new ServiceHealthcheck(Name, ServiceHealthcheck.Status.Unknown, LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION))

  val iso8601DateFormat: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSXXX").withZone(ZoneId.systemDefault())

  val readsOffsetDateTime: Reads[OffsetDateTime] = implicitly[Reads[String]].map(OffsetDateTime.parse(_, iso8601DateFormat))

  sealed abstract class StatusPageStatus(override val entryName: String, val healthcheckStatus: ServiceHealthcheck.Status) extends EnumEntry
  object StatusPageStatus extends Enum[StatusPageStatus] with PlayJsonEnum[StatusPageStatus] {
    case object Operational extends StatusPageStatus("operational", ServiceHealthcheck.Status.Okay)
    case object DegradedPerformance extends StatusPageStatus("degraded_performance", ServiceHealthcheck.Status.Warning)
    case object PartialOutage extends StatusPageStatus("partial_outage", ServiceHealthcheck.Status.Warning)
    case object MajorOutage extends StatusPageStatus("major_outage", ServiceHealthcheck.Status.Error)

    override def values: IndexedSeq[StatusPageStatus] = findValues
  }

  case class StatusPageComponent(
    id: String,
    name: String,
    status: StatusPageStatus,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime
  )
  val readsStatusPageComponent: Reads[StatusPageComponent] = (
    (__ \ "id").read[String] and
    (__ \ "name").read[String] and
    (__ \ "status").read[StatusPageStatus] and
    (__ \ "created_at").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "updated_at").read[OffsetDateTime](readsOffsetDateTime)
  )(StatusPageComponent.apply _)

  sealed trait StatusPageIncidentStatus extends EnumEntry
  object StatusPageIncidentStatus extends Enum[StatusPageIncidentStatus] {
    case object Investigating extends StatusPageIncidentStatus
    case object Identified extends StatusPageIncidentStatus
    case object Monitoring extends StatusPageIncidentStatus
    case object Resolved extends StatusPageIncidentStatus
    case object Postmortem extends StatusPageIncidentStatus

    override def values: IndexedSeq[StatusPageIncidentStatus] = findValues
  }
  val readsStatusPageIncidentStatus: Reads[StatusPageIncidentStatus] = EnumFormats.formats(StatusPageIncidentStatus, insensitive = true)

  sealed trait StatusPageIncidentImpact extends EnumEntry
  object StatusPageIncidentImpact extends Enum[StatusPageIncidentImpact] {
    case object None extends StatusPageIncidentImpact
    case object Minor extends StatusPageIncidentImpact
    case object Major extends StatusPageIncidentImpact
    case object Critical extends StatusPageIncidentImpact

    override def values: IndexedSeq[StatusPageIncidentImpact] = findValues
  }
  val readsStatusPageIncidentImpact: Reads[StatusPageIncidentImpact] = EnumFormats.formats(StatusPageIncidentImpact, insensitive = true)

  case class StatusPageIncident(
    id: String,
    name: String,
    status: StatusPageIncidentStatus,
    createdAt: OffsetDateTime,
    updatedAt: OffsetDateTime,
    impact: StatusPageIncidentImpact,
    shortlink: String,
    startedAt: OffsetDateTime,
    components: Seq[StatusPageComponent]
  )
  val readsStatusPageIncident: Reads[StatusPageIncident] = (
    (__ \ "id").read[String] and
    (__ \ "name").read[String] and
    (__ \ "status").read[StatusPageIncidentStatus](readsStatusPageIncidentStatus) and
    (__ \ "created_at").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "updated_at").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "impact").read[StatusPageIncidentImpact](readsStatusPageIncidentImpact) and
    (__ \ "shortlink").read[String] and
    (__ \ "started_at").read[OffsetDateTime](readsOffsetDateTime) and
    (__ \ "components").read[Seq[StatusPageComponent]](Reads.seq(readsStatusPageComponent))
  )(StatusPageIncident.apply _)
}

@Component
@Profile(Array("scheduling"))
class TurnitinStatusHealthcheck extends ServiceHealthcheckProvider(InitialState)
  with AutowiringApacheHttpClientComponent
  with Logging {

  private lazy val turnitinStatusComponentsEndpoint: String = Wire.property("${turnitin.status.componentsEndpoint}")
  private lazy val turnitinStatusIncidentsEndpoint: String = Wire.property("${turnitin.status.incidentsEndpoint}")
  private lazy val interestedComponents: Seq[String] = Wire.property("${turnitin.status.interestedComponents}").split(',').toSeq

  @Scheduled(fixedRate = 60 * 1000) // 1 minute
  override def run(): Unit = {
    val statusRequest = RequestBuilder.get(turnitinStatusComponentsEndpoint).build()

    val statusHandler: ResponseHandler[JsResult[Seq[StatusPageComponent]]] =
      ApacheHttpClientUtils.jsonResponseHandler { json =>
        (json \ "components").validate[Seq[StatusPageComponent]](Reads.seq(readsStatusPageComponent))
          .map(_.filter(c => interestedComponents.contains(c.id)))
      }

    Try(httpClient.execute(statusRequest, statusHandler)).fold(
      t => {
        update(new ServiceHealthcheck(
          Name,
          ServiceHealthcheck.Status.Unknown,
          LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
          s"There was an error fetching Turnitin component status: ${t.getMessage}"
        ))
      },
      _.fold(
        invalid =>
          update(new ServiceHealthcheck(
            Name,
            ServiceHealthcheck.Status.Unknown,
            LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
            s"There was an error parsing the Turnitin component status as JSON: $invalid"
          )),

        components => {
          val status = components.map(_.status.healthcheckStatus).maxBy(_.ordinal())
          val message = components.map(c => s"${c.name} is ${c.status}${if (c.status.healthcheckStatus == ServiceHealthcheck.Status.Warning) " (!)" else if (c.status.healthcheckStatus == ServiceHealthcheck.Status.Error) " (!!)" else ""}").mkString(", ")

          if (status == ServiceHealthcheck.Status.Okay) {
            update(new ServiceHealthcheck(
              Name,
              status,
              LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
              message
            ))
          } else {
            val incidentsRequest = RequestBuilder.get(turnitinStatusIncidentsEndpoint).build()

            val incidentsHandler: ResponseHandler[JsResult[Seq[StatusPageIncident]]] =
              ApacheHttpClientUtils.jsonResponseHandler { json =>
                (json \ "incidents").validate[Seq[StatusPageIncident]](Reads.seq(readsStatusPageIncident))
                  .map(_.filter(i => i.components.exists(c => interestedComponents.contains(c.id))))
              }

            Try(httpClient.execute(incidentsRequest, incidentsHandler)).fold(
              t => {
                update(new ServiceHealthcheck(
                  Name,
                  status,
                  LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
                  s"$message; there was an error getting incident details: ${t.getMessage}"
                ))
              },
              _.fold(
                invalid =>
                  update(new ServiceHealthcheck(
                    Name,
                    status,
                    LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
                    s"$message; there was an error parsing the Turnitin incidents as JSON: $invalid"
                  )),
                incidents => {
                  val incidentsMessage = incidents.map(i => s"${i.name} (${i.status}, ${i.impact}): ${i.shortlink}").mkString(", ")

                  update(new ServiceHealthcheck(
                    Name,
                    status,
                    LocalDateTime.now(DateTimeUtils.CLOCK_IMPLEMENTATION),
                    s"$message; $incidentsMessage"
                  ))
                }
              )
            )
          }
        }
      )
    )
  }

}
