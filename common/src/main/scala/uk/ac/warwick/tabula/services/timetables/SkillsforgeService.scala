package uk.ac.warwick.tabula.services.timetables


import java.nio.charset.Charset

import com.google.common.base.Charsets
import org.apache.http.{HttpEntity, HttpResponse}
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.RequestBuilder
import org.apache.http.impl.client.BasicResponseHandler
import org.apache.http.util.EntityUtils
import org.joda.time.{LocalDate, LocalDateTime}
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.{CurrentUser, RequestFailedException}
import uk.ac.warwick.tabula.data.model.{Location, Member}
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services.{ApacheHttpClientComponent, UserLookupComponent}
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.{EventList, EventOccurrenceList}
import uk.ac.warwick.tabula.timetables.{EventOccurrence, RelatedUrl, TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.userlookup.User

import scala.concurrent.Future
import scala.util.control.NonFatal
import scala.util.{Failure, Success, Try}

trait SkillsforgeServiceComponent {
	self: SkillsForgeConfigurationComponent with ApacheHttpClientComponent =>

	def skillsforge: SkillsforgeService = new SkillsforgeService

	class SkillsforgeService
		extends EventOccurrenceSource
			with Logging {

		val dateParameterFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd-MMM-yyyy")

		override def occurrencesFor(member: Member, currentUser: CurrentUser, context: TimetableEvent.Context, start: LocalDate, end: LocalDate): Future[EventOccurrenceList] = Future {
			val req = RequestBuilder.get(s"${config.baseUri}/${member.userId}")
				.addHeader("X-Auth-Token", config.authToken)
				.addParameter("start", dateParameterFormatter.print(start))
				.addParameter("end", dateParameterFormatter.print(end))
				.build()

			try {
				val data: String = httpClient.execute(req, (res: HttpResponse) => {
					EntityUtils.toString(res.getEntity, Charsets.UTF_8)
				})

				//logger.info(s"Skillsforge data for user ${member.userId}: $data")
				// TODO map `data` to EventOccurrenceList (NEWSTART-1626 has example data for student4)
				// TODO is this cached?

				// A pretend event to show that this source is coming through
				EventOccurrenceList(Seq(
					EventOccurrence(
						uid = "hellotest",
						name = "Today",
						title = "Today",
						description = "It is today",
						eventType = TimetableEventType.Other("Skillsforge"),
						start = LocalDateTime.now(),
						end = LocalDateTime.now().plusHours(1),
						location = None,
						parent = Parent(),
						comments = None,
						staff = Nil,
						relatedUrl = None,
						attendance = None
					)
				), None)
			} catch {
				case NonFatal(e) =>
					throw new RequestFailedException("The Skillsforge service could not be reached", e)
			}
		}
	}
}



case class SkillsforgeConfiguration (
	baseUri: String,
	authToken: String
)

trait SkillsForgeConfigurationComponent {
	def config: SkillsforgeConfiguration
}

trait AutowiringSkillsforgeConfigurationComponent extends SkillsForgeConfigurationComponent {
	lazy val config = SkillsforgeConfiguration(
		baseUri = Wire.property("${skillsforge.base.url}"),
		authToken = Wire.property("${skillsforge.authToken}")
	)
}

