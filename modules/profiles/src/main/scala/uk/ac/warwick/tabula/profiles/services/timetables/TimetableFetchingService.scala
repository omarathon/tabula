package uk.ac.warwick.tabula.profiles.services.timetables

import org.springframework.beans.factory.DisposableBean
import dispatch.classic._
import dispatch.classic.Request.toRequestVerbs
import uk.ac.warwick.tabula.helpers.{ClockComponent, Logging}
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.params.CookiePolicy
import dispatch.classic.thread.ThreadSafeHttpClient
import scala.xml.Elem
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import scala.annotation.meta.param
import scala.util.{Success, Try}
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}

trait TimetableFetchingService {
	def getTimetableForStudent(universityId: String): Seq[TimetableEvent]
	def getTimetableForModule(moduleCode: String): Seq[TimetableEvent]
	def getTimetableForCourse(courseCode: String): Seq[TimetableEvent]
	def getTimetableForRoom(roomName: String): Seq[TimetableEvent]
	def getTimetableForStaff(universityId: String): Seq[TimetableEvent]
}

trait TimetableFetchingServiceComponent {
	def timetableFetchingService:TimetableFetchingService

}

trait ScientiaConfigurationComponent{
	val scientiaConfiguration:ScientiaConfiguration
	trait ScientiaConfiguration{
		val perYearUris:Seq[(String, AcademicYear)]
	}
}
trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent with ClockComponent{
	val scientiaConfiguration = new AutowiringScientiaConfiguration
	class AutowiringScientiaConfiguration extends ScientiaConfiguration{
		def scientiaFormat(year:AcademicYear) = {
				// e.g. 1314
				(year.startYear%100).toString +(year.endYear%100).toString
		}

		lazy val scientiaBaseUrl = Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml")
		lazy val currentAcademicYear = AcademicYear.guessByDate(clock.now)
		lazy val prevAcademicYear = currentAcademicYear.-(1)
		lazy val perYearUris =	Seq(prevAcademicYear, currentAcademicYear) map (year=>(scientiaBaseUrl + scientiaFormat(year) + "/",year))
	}
}

trait ScientiaHttpTimetableFetchingServiceComponent extends TimetableFetchingServiceComponent{

	this:ScientiaConfigurationComponent =>

	lazy val timetableFetchingService = {
		if (scientiaConfiguration.perYearUris.exists(_._1.contains("stubTimetable")))
		{
			// don't cache if we're using the test stub - otherwise we won't see updates that the test setup makes
			new ScientiaHttpTimetableFetchingService
		}else{
			new CachedTimetableFetchingService(new ScientiaHttpTimetableFetchingService)
		}
	}



	class ScientiaHttpTimetableFetchingService extends TimetableFetchingService with Logging with DisposableBean {
		import ScientiaHttpTimetableFetchingService._

		lazy val perYearUris = scientiaConfiguration.perYearUris

		lazy val studentUris = perYearUris.map {
			case (uri, year) => (uri + "?StudentXML", year)
		}
		lazy val staffUris = perYearUris.map {
			case (uri, year) => (uri + "?StaffXML", year)
		}
		lazy val courseUris = perYearUris.map {
			case (uri, year) => (uri + "?CourseXML", year)
		}
		lazy val moduleUris = perYearUris.map {
			case (uri, year) => (uri + "?ModuleXML", year)
		}
		lazy val roomUris = perYearUris.map {
			case (uri, year) => (uri + "?RoomXML", year)
		}

		val http: Http = new Http with thread.Safety {
			override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
				getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
			}
		}

		override def destroy {
			http.shutdown()
		}

		// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
		// the timetable response doesn't include its year, so we pass that in separately.
		def handler(year:AcademicYear) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
			req <> { (node) => parseXml(node, year) }
		}

		def getTimetableForStudent(universityId: String): Seq[TimetableEvent] = doRequest(studentUris, universityId)
		def getTimetableForModule(moduleCode: String): Seq[TimetableEvent] = doRequest(moduleUris, moduleCode)
		def getTimetableForCourse(courseCode: String): Seq[TimetableEvent] = doRequest(courseUris, courseCode)
		def getTimetableForRoom(roomName: String): Seq[TimetableEvent] = doRequest(roomUris, roomName)
		def getTimetableForStaff(universityId: String): Seq[TimetableEvent] = doRequest(staffUris, universityId)

		def doRequest(uris: Seq[(String, AcademicYear)], param: String):Seq[TimetableEvent] = {
			// fetch the events from each of the supplied URIs, and flatmap them to make one big list of events
			uris.flatMap{case (uri, year) => {
				// add ?p0={param} to the URL's get parameters
				val req = url(uri) <<? Map("p0" -> param)
				// execute the request.
				// If the status is OK, pass the response to the handler function for turning into TimetableEvents
				// else return an empty list.
				logger.info(s"Requesting timetable data from $uri")
				Try(http.when(_==200)(req >:+ handler(year))) match {
					case Success(ev)=>ev
					case _ => Nil
				}
			}}
		}

	}
}
object ScientiaHttpTimetableFetchingService {
	
	def parseXml(xml: Elem, year:AcademicYear): Seq[TimetableEvent] =
		xml \\ "Activity" map { activity => 
			TimetableEvent(
				name = (activity \\ "name").text,
				description = (activity \\ "description").text,
				eventType = TimetableEventType((activity \\ "type").text),
				weekRanges = new WeekRangeListUserType().convertToObject((activity \\ "weeks").text),
				day = DayOfWeek.apply((activity \\ "day").text.toInt + 1),
				startTime = new LocalTime((activity \\ "start").text),
				endTime = new LocalTime((activity \\ "end").text),
				location = (activity \\ "room").text match {
					case text if !text.isEmpty => {
						// S+ has some (not all) rooms as "AB_AB1.2", where AB is a building code
						// we're generally better off without this.
						val removeBuildingNames = "^[^_]*_".r
						Some(removeBuildingNames.replaceFirstIn(text,""))
					}
					case _ => None
				},
				context = Option((activity \\ "module").text),
				staffUniversityIds = (activity \\ "staffmember") map { _.text },
				year = year
			)
		}
	
}