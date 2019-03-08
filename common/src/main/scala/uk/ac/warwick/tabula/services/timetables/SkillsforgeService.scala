package uk.ac.warwick.tabula.services.timetables

import com.google.common.base.Charsets
import org.apache.http.HttpResponse
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.util.EntityUtils
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import org.joda.time.{DateTime, LocalDate}
import play.api.libs.json._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.{Location, Member, NamedLocation}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.ApacheHttpClientComponent
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventOccurrenceList
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.tabula.timetables.{EventOccurrence, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{CurrentUser, RequestFailedException}

import scala.collection.Seq
import scala.concurrent.Future
import scala.util.control.NonFatal

trait SkillsforgeServiceComponent extends EventOccurrenceSourceComponent {
	self: SkillsForgeConfigurationComponent
		with ApacheHttpClientComponent =>

	override def eventOccurrenceSource: EventOccurrenceSource = new SkillsforgeService

	class SkillsforgeService
		extends EventOccurrenceSource
			with Logging {

		val dateParameterFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd-MMM-yyyy")

		// TAB-6942
		def existsInSkillsforge(member: Member): Boolean =
			member.isPgr || (member.homeDepartment.code == "ec" && member.isPgt)

		override def occurrencesFor(
				member: Member,
				currentUser: CurrentUser,
				context: TimetableEvent.Context,
				start: LocalDate,
				endInclusive: LocalDate): Future[EventOccurrenceList] =
			if (existsInSkillsforge(member)) {
				Future {

					// For testing
					config.hardcodedUserId.foreach { id =>
						logger.info(s"Fetching skillsforge data for hardcoded ID $id")
					}
					val userId = config.hardcodedUserId.getOrElse(member.userId)
					val end = endInclusive.plusDays(1) // Skillsforge is exclusive, we're inclusive

					val req = RequestBuilder.post(s"${config.baseUri}/$userId")
						.addHeader("X-Auth-Token", config.authToken)
						.addParameter("start", dateParameterFormatter.print(start))
						.addParameter("end", dateParameterFormatter.print(end))
						.build()

					try {
						val data: JsObject = httpClient.execute(req, (res: HttpResponse) => {
							Json.parse(EntityUtils.toString(res.getEntity, Charsets.UTF_8)).as[JsObject]
						})

						val occurrences: Seq[EventOccurrence] = if ((data \ "success").as[Boolean]) {
							(data \ "data").as[Seq[JsObject]].map(Skillsforge.toEventOccurrence)
						} else {
							val errorMessage = (data \ "errorMessage").asOpt[String].getOrElse(s"Unknown error from Skillsforge: $data")
							/*
								The API will return this error if a user doesn't exist:

									{"success":false,"data":null,"errorMessage":"Could not retrieve users bookings - EMBD.GBRFU:1.3: Could not complete operation: Insufficient privileges."}

								 However, it will also return this exact same error if you don't have permission (such as when the auth token is incorrect).
								 I did query this but the developers said this is just how it works.

								 I've made a flag which will be set true on one of the instances, so hopefully if there are problems with the auth token we will
								 see them there without bogging down the logs elsewhere.
							*/
							if (errorMessage.contains("Insufficient privileges") && !config.reportAuthErrors) {
								// Now that we are limiting this request just to users we actually expect to be in Skillsforge, we are
								// logging at WARN level so it's more visible.
								logger.warn(s"Ignoring error for Usercode($userId) because it is probably just a nonexistent user: $errorMessage")
								Nil
							} else {
								throw new RuntimeException(s"Skillsforge error: $errorMessage")
							}
						}

						EventOccurrenceList(occurrences, Some(new DateTime()))
					} catch {
						case NonFatal(e) =>
							throw new RequestFailedException("The Skillsforge service could not be reached", e)
					}
				}
			} else {
				Future.successful(EventOccurrenceList.empty)
			}

	}
}


case class SkillsforgeConfiguration (
	baseUri: String,
	authToken: String,
	hardcodedUserId: Option[String],
	reportAuthErrors: Boolean,
)

trait SkillsForgeConfigurationComponent {
	def config: SkillsforgeConfiguration
}

trait AutowiringSkillsforgeConfigurationComponent extends SkillsForgeConfigurationComponent {
	lazy val config = SkillsforgeConfiguration(
		baseUri = Wire.property("${skillsforge.base.url}"),
		authToken = Wire.property("${skillsforge.authToken}"),
		hardcodedUserId = Wire.optionProperty("${skillsforge.hardcodedUserId}").filter(_.hasText),
		reportAuthErrors = Wire.property("${skillsforge.reportAuthErrors}").toBoolean,
	)
}

object Skillsforge {

	// Joda can parse the incoming dates with its default parser, but when this is converted
	// to Java Time it will likely need a custom pattern to handle the +HHMM zone offset at the end,
	// which it doesn't seem able to handle by default.
	implicit val dateTimeReads: Reads[DateTime] = JodaReads.DefaultJodaDateTimeReads

	def toEventOccurrence(obj: JsObject): EventOccurrence = {
		val startDate = (obj \ "eventStartIsoDate").as[DateTime].toLocalDateTime
		val endDate = (obj \ "eventEndIsoDate").as[DateTime].toLocalDateTime

		EventOccurrence(
			uid = (obj \ "bookingId").as[String],
			name = "Skillsforge event",
			title = (obj \ "eventTitle").as[String],
			description = "",
			eventType = TimetableEventType.Other("Skillsforge"),
			start = startDate,
			end = endDate,
			location = (obj \ "venue").asOpt[JsObject].map(toLocation),
			parent = Parent(),
			comments = None,
			staff = Nil,
			relatedUrl = None,
			attendance = None
		)
	}

	def toLocation(obj: JsObject): Location = NamedLocation(
		(obj \ "detailedLocation").asOpt[String].getOrElse {
			(obj \ "displayString").as[String]
		}
	)


}

