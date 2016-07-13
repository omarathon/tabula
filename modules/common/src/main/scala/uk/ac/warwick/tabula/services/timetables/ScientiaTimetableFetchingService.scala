package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.url
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.{DateTimeConstants, LocalTime}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRangeListUserType}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{ClockComponent, FoundUser, Futures, Logging}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.concurrent.Future
import scala.xml.Elem

trait ScientiaConfiguration {
	val perYearUris: Seq[(String, AcademicYear)]
	val cacheSuffix: String
}

trait ScientiaConfigurationComponent {
	val scientiaConfiguration: ScientiaConfiguration
}

trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent with ClockComponent {
	val scientiaConfiguration = new AutowiringScientiaConfiguration

	class AutowiringScientiaConfiguration extends ScientiaConfiguration {
		def scientiaFormat(year: AcademicYear) = {
			// e.g. 1314
			(year.startYear % 100).toString + (year.endYear % 100).toString
		}

		lazy val scientiaBaseUrl = Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml")
		lazy val currentAcademicYear: Option[AcademicYear] = Some(AcademicYear.guessSITSAcademicYearByDate(clock.now))
		lazy val prevAcademicYear: Option[AcademicYear] = {
			// TAB-3074 we only fetch the previous academic year if the month is >= AUGUST and < OCTOBER
			val month = clock.now.getMonthOfYear
			if (month >= DateTimeConstants.AUGUST && month < DateTimeConstants.OCTOBER)
				currentAcademicYear.map(_ - 1)
			else
				None
		}

		def yearProperty: Option[Seq[AcademicYear]] =
			Wire.optionProperty("${scientia.years}").map { _.split(",").map(AcademicYear.parse) }

		lazy val perYearUris =
			yearProperty.getOrElse { Seq(prevAcademicYear, currentAcademicYear).flatten }
				.map { year => (scientiaBaseUrl + scientiaFormat(year) + "/", year) }

		lazy val cacheSuffix = Wire.optionProperty("${scientia.cacheSuffix}").getOrElse("")
	}

}

trait ScientiaHttpTimetableFetchingServiceComponent extends CompleteTimetableFetchingServiceComponent {
	self: ScientiaConfigurationComponent =>

	lazy val timetableFetchingService = ScientiaHttpTimetableFetchingService(scientiaConfiguration)
}

private class ScientiaHttpTimetableFetchingService(scientiaConfiguration: ScientiaConfiguration) extends CompleteTimetableFetchingService with Logging {
	self: LocationFetchingServiceComponent
		with SmallGroupServiceComponent
		with ModuleAndDepartmentServiceComponent
		with UserLookupComponent
		with DispatchHttpClientComponent =>

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

	// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
	// the timetable response doesn't include its year, so we pass that in separately.
	def handler(year: AcademicYear, excludeSmallGroupEventsInTabula: Boolean = false, uniId: String) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req <> { node =>
			parseXml(node, year, uniId, locationFetchingService, moduleAndDepartmentService, userLookup)
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

	def doRequest(uris: Seq[(String, AcademicYear)], param: String, excludeSmallGroupEventsInTabula: Boolean = false): Future[EventList] = {
		// fetch the events from each of the supplied URIs, and flatmap them to make one big list of events
		val results: Seq[Future[EventList]] = uris.map { case (uri, year) =>
			// add ?p0={param} to the URL's get parameters
			val req = url(uri) <<? Map("p0" -> param)
			// execute the request.
			// If the status is OK, pass the response to the handler function for turning into TimetableEvents
			// else return an empty list.
			logger.info(s"Requesting timetable data from ${req.to_uri.toString}")

			val result = Future {
				val ev = httpClient.when(_==200)(req >:+ handler(year, excludeSmallGroupEventsInTabula, param))

				if (ev.isEmpty) {
					logger.info(s"Timetable request successful but no events returned: ${req.to_uri.toString}")
					throw new TimetableEmptyException(uri, param)
				}

				ev
			}

			// Some extra logging here
			result.onFailure { case e =>
				logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
			}

			result.map { events =>
				if (excludeSmallGroupEventsInTabula)
					EventList.fresh(events.filterNot { event =>
						event.eventType == TimetableEventType.Seminar &&
							hasSmallGroups(event.parent.shortName, year)
					})
				else EventList.fresh(events)
			}
		}

		Futures.combine(results, EventList.combine)
	}

}

class TimetableEmptyException(val uri: String, val param: String)
	extends IllegalStateException(s"Received an empty timetable from $uri for $param")

object ScientiaHttpTimetableFetchingService extends Logging {

	val cacheName = "SyllabusPlusTimetableLists"

	def apply(scientiaConfiguration: ScientiaConfiguration) = {
		val service =
			new ScientiaHttpTimetableFetchingService(scientiaConfiguration)
				with WAI2GoHttpLocationFetchingServiceComponent
				with AutowiringSmallGroupServiceComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with AutowiringWAI2GoConfigurationComponent
				with AutowiringUserLookupComponent
				with AutowiringDispatchHttpClientComponent

		if (scientiaConfiguration.perYearUris.exists(_._1.contains("stubTimetable"))) {
			// don't cache if we're using the test stub - otherwise we won't see updates that the test setup makes
			service
		} else {
			new CachedCompleteTimetableFetchingService(service, s"$cacheName${scientiaConfiguration.cacheSuffix}")
		}
	}

	def parseXml(
		xml: Elem,
		year: AcademicYear,
		uniId: String,
		locationFetchingService: LocationFetchingService,
		moduleAndDepartmentService: ModuleAndDepartmentService,
		userLookup: UserLookupService
	): Seq[TimetableEvent] = {
		val moduleCodes = (xml \\ "module").map(_.text.toLowerCase).distinct
		if (moduleCodes.isEmpty) logger.info(s"No modules returned for: $uniId")
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
				staff = userLookup.getUsersByWarwickUniIds((activity \\ "staffmember") map {
					_.text
				}).values.collect { case FoundUser(u) => u }.toSeq,
				students = userLookup.getUsersByWarwickUniIds((activity \\ "student") map {
					_.text
				}).values.collect { case FoundUser(u) => u }.toSeq,
				year = year,
				relatedUrl = None
			)
		}
	}
}