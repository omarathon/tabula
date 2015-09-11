package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic.{url, thread, Http}
import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.joda.time.{DateTimeConstants, LocalTime}
import org.springframework.beans.factory.DisposableBean
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRangeListUserType}
import uk.ac.warwick.tabula.helpers.{Logging, ClockComponent}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.timetables.TimetableEvent.Parent
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}

import scala.util.{Failure, Success, Try}
import scala.xml.Elem

trait ScientiaConfiguration {
	val perYearUris: Seq[(String, AcademicYear)]
}

trait ScientiaConfigurationComponent {
	val scientiaConfiguration: ScientiaConfiguration
}

trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent with ClockComponent{
	val scientiaConfiguration = new AutowiringScientiaConfiguration
	class AutowiringScientiaConfiguration extends ScientiaConfiguration {
		def scientiaFormat(year:AcademicYear) = {
			// e.g. 1314
			(year.startYear%100).toString +(year.endYear%100).toString
		}

		lazy val scientiaBaseUrl = Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml")
		lazy val currentAcademicYear: Option[AcademicYear] = Some(AcademicYear.guessSITSAcademicYearByDate(clock.now))
		lazy val prevAcademicYear: Option[AcademicYear] = {
			// TAB-3074 we only fetch the previous academic year if the month is >= AUGUST and < NOVEMBER
			val month = clock.now.getMonthOfYear
			if (month >= DateTimeConstants.AUGUST && month < DateTimeConstants.NOVEMBER)
				currentAcademicYear.map { _ - 1 }
			else
				None
		}
		lazy val perYearUris =	Seq(prevAcademicYear, currentAcademicYear).flatten map (year=>(scientiaBaseUrl + scientiaFormat(year) + "/",year))
	}
}

trait ScientiaHttpTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent {
	self: ScientiaConfigurationComponent =>

	lazy val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)
}

private class ScientiaHttpTimetableFetchingService(scientiaConfiguration: ScientiaConfiguration) extends CompleteTimetableFetchingService with Logging with DisposableBean {
	self: LocationFetchingServiceComponent with SmallGroupServiceComponent with ModuleAndDepartmentServiceComponent =>

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
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy {
		http.shutdown()
	}

	// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
	// the timetable response doesn't include its year, so we pass that in separately.
	def handler(year: AcademicYear, excludeSmallGroupEventsInTabula: Boolean = false) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req <> { node =>
			val events = parseXml(node, year, locationFetchingService, moduleAndDepartmentService)

			if (excludeSmallGroupEventsInTabula)
				events.filterNot { event =>
					event.eventType == TimetableEventType.Seminar &&
					hasSmallGroups(event.parent.shortName, year)
				}
			else events
		}
	}

	private def hasSmallGroups(moduleCode: Option[String], year: AcademicYear) =
		moduleCode.flatMap(moduleAndDepartmentService.getModuleByCode).fold(false) { module =>
			!smallGroupService.getSmallGroupSets(module, year).forall(_.archived)
		}

	def getTimetableForStudent(universityId: String) = doRequest(studentUris, universityId, excludeSmallGroupEventsInTabula = true)
	def getTimetableForModule(moduleCode: String) = doRequest(moduleUris, moduleCode)
	def getTimetableForCourse(courseCode: String) = doRequest(courseUris, courseCode)
	def getTimetableForRoom(roomName: String) = doRequest(roomUris, roomName)
	def getTimetableForStaff(universityId: String) = doRequest(staffUris, universityId, excludeSmallGroupEventsInTabula = true)

	def doRequest(uris: Seq[(String, AcademicYear)], param: String, excludeSmallGroupEventsInTabula: Boolean = false): Try[Seq[TimetableEvent]] = {
		def flatten[T](xs: Seq[Try[Seq[T]]]): Try[Seq[T]] = {
			val (ss: Seq[Success[Seq[T]]] @unchecked, fs: Seq[Failure[Seq[T]]] @unchecked) =
				xs.partition(_.isSuccess)

			if (fs.isEmpty) Success(ss.flatMap(_.get))
			else Failure[Seq[T]](fs.head.exception) // Only keep the first failure
		}

		// fetch the events from each of the supplied URIs, and flatmap them to make one big list of events
		val results = uris.map { case (uri, year) =>
			// add ?p0={param} to the URL's get parameters
			val req = url(uri) <<? Map("p0" -> param)
			// execute the request.
			// If the status is OK, pass the response to the handler function for turning into TimetableEvents
			// else return an empty list.
			logger.info(s"Requesting timetable data from $uri")

			val result = Try(http.when(_==200)(req >:+ handler(year, excludeSmallGroupEventsInTabula)))

			// Some extra logging here
			result match {
				case Success(ev) =>
					if (ev.isEmpty) logger.info("Timetable request successful but no events returned")
				case Failure(e) =>
					logger.warn(s"Request for $uri failed: ${e.getMessage}")
			}

			result
		}

		flatten(results)
	}

}

object ScientiaHttpTimetableFetchingService {

	val cacheName = "SyllabusPlusTimetables"

	def apply(scientiaConfiguration: ScientiaConfiguration) = {
		val service = new ScientiaHttpTimetableFetchingService(scientiaConfiguration) with WAI2GoHttpLocationFetchingServiceComponent with AutowiringSmallGroupServiceComponent with AutowiringModuleAndDepartmentServiceComponent with AutowiringWAI2GoConfigurationComponent

		if (scientiaConfiguration.perYearUris.exists(_._1.contains("stubTimetable"))) {
			// don't cache if we're using the test stub - otherwise we won't see updates that the test setup makes
			service
		} else {
			new CachedCompleteTimetableFetchingService(service, cacheName)
		}
	}

	def parseXml(
		xml: Elem,
		year: AcademicYear,
		locationFetchingService: LocationFetchingService,
		moduleAndDepartmentService: ModuleAndDepartmentService
	): Seq[TimetableEvent] = {
		val moduleCodes = (xml \\ "module").map(_.text.toLowerCase).distinct
		val moduleMap = moduleAndDepartmentService.getModulesByCodes(moduleCodes).groupBy(_.code).mapValues(_.head)
		xml \\ "Activity" map { activity =>
			val name = (activity \\ "name").text

			val startTime = new LocalTime((activity \\ "start").text)
			val endTime = new LocalTime((activity \\ "end").text)

			val location = (activity \\ "room").text match {
				case text if !text.isEmpty =>
					// S+ has some (not all) rooms as "AB_AB1.2", where AB is a building code
					// we're generally better off without this.
					val removeBuildingNames = "^[^_]*_".r
					Some(locationFetchingService.locationFor(removeBuildingNames.replaceFirstIn(text, "")))
				case _ => None
			}

			val parent = TimetableEvent.Parent(moduleMap.get((activity \\ "module").text.toLowerCase))

			val dayOfWeek = DayOfWeek.apply((activity \\ "day").text.toInt + 1)

			val uid =
				DigestUtils.md5Hex(
					Seq(
						name,
						startTime.toString,
						endTime.toString,
						dayOfWeek.toString,
						location.fold("") {
							_.name
						},
						parent.shortName.getOrElse("")
					).mkString
				)

			TimetableEvent(
				uid = uid,
				name = name,
				title = (activity \\ "title").text,
				description = (activity \\ "description").text,
				eventType = TimetableEventType((activity \\ "type").text),
				weekRanges = new WeekRangeListUserType().convertToObject((activity \\ "weeks").text),
				day = dayOfWeek,
				startTime = startTime,
				endTime = endTime,
				location = location,
				comments = Option((activity \\ "comments").text).flatMap {
					_.maybeText
				},
				parent = parent,
				staffUniversityIds = (activity \\ "staffmember") map {
					_.text
				},
				studentUniversityIds = (activity \\ "student") map {
					_.text
				},
				year = year
			)
		}
	}
}