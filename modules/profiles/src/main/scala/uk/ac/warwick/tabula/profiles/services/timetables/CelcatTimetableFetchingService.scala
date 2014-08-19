package uk.ac.warwick.tabula.profiles.services.timetables

import java.io.InputStream

import dispatch.classic.thread.ThreadSafeHttpClient
import dispatch.classic._
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.{RRule, DateProperty, Categories}
import net.fortuna.ical4j.model.{Parameter, Property, Component}
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.util.CompatibilityHints
import org.apache.http.auth.AuthScope
import org.apache.http.client.params.{CookiePolicy, ClientPNames}
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.beans.factory.DisposableBean
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.{WeekRange, DayOfWeek}
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{TermServiceComponent, AutowiringTermServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.timetables.{TimetableEventType, TimetableEvent}

import scala.collection.JavaConverters._
import scala.util.{Success, Try}

trait CelcatConfiguration {
	val perDepartmentBaseUris: Seq[(String, String)]
	val authScope: AuthScope
	val credentials: Credentials
	val cacheEnabled: Boolean
}

trait CelcatConfigurationComponent {
	val celcatConfiguration: CelcatConfiguration
}

trait AutowiringCelcatConfigurationComponent extends CelcatConfigurationComponent {
	val celcatConfiguration = new AutowiringCelcatConfiguration
	class AutowiringCelcatConfiguration extends CelcatConfiguration {
		val perDepartmentBaseUris =	Seq(
			("ch", "https://www2.warwick.ac.uk/appdata/chem-timetables"),
			("es", "https://www2.warwick.ac.uk/appdata/eng-timetables")
		)
		lazy val authScope = new AuthScope("www2.warwick.ac.uk", 443)
		lazy val credentials = Credentials(Wire.property("${celcat.fetcher.username}"), Wire.property("${celcat.fetcher.password}"))
		val cacheEnabled = true
	}
}

trait CelcatHttpTimetableFetchingServiceComponent extends StudentTimetableFetchingServiceComponent {
	self: CelcatConfigurationComponent =>

	lazy val timetableFetchingService = CelcatHttpTimetableFetchingService(celcatConfiguration)
}

object CelcatHttpTimetableFetchingService {
	val cacheName = "CelcatTimetables"

	def apply(celcatConfiguration: CelcatConfiguration) = {
		val delegate = new CelcatHttpTimetableFetchingService(celcatConfiguration) with AutowiringUserLookupComponent with AutowiringTermServiceComponent

		if (celcatConfiguration.cacheEnabled) {
			new CachedStudentTimetableFetchingService(delegate, cacheName)
		} else {
			delegate
		}
	}
}

class CelcatHttpTimetableFetchingService(celcatConfiguration: CelcatConfiguration) extends StudentTimetableFetchingService with Logging with DisposableBean {
	self: UserLookupComponent with TermServiceComponent =>

	lazy val baseUris = celcatConfiguration.perDepartmentBaseUris.toMap

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(Some(celcatConfiguration.authScope, celcatConfiguration.credentials)), maxConnections, maxConnectionsPerRoute) {
			getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy {
		http.shutdown()
	}

	// a dispatch response handler which reads iCal from the response and parses it into a list of TimetableEvents
	def handler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req >> { (is) => combineIdenticalEvents(parseICal(is)) }
	}

	def getTimetableForStudent(universityId: String): Seq[TimetableEvent] = doRequest(universityId)

	def doRequest(universityId: String): Seq[TimetableEvent] = {
		userLookup.getUserByWarwickUniId(universityId) match {
			case FoundUser(u) => baseUris.get(u.getDepartmentCode.toLowerCase).map { baseUri =>
				// Add {universityId}.ics to the URL
				val req = (url(baseUri) / s"${u.getWarwickId}.ics" <<? Map("forcebasic" -> "true"))

				// Execute the request
				// If the status is OK, pass the response to the handler function for turning into TimetableEvents
				// else return an empty list.
				logger.info(s"Requesting timetable data from $req")
				Try(http.when(_==200)(req >:+ handler)) match {
					case Success(ev) => ev
					case _ => Nil
				}
			}.getOrElse(Nil)
			case _ => Nil
		}
	}

	def combineIdenticalEvents(events: Seq[TimetableEvent]): Seq[TimetableEvent] = {
		// If we run an identical event in separate weeks, combine the weeks for them
		val groupedEvents = events.groupBy { event =>
			(event.name, event.title, event.description, event.eventType, event.day, event.startTime, event.endTime,
				event.location, event.context, event.staffUniversityIds, event.year)
		}.values.toSeq

		groupedEvents.map {
			case event :: Nil => event
			case events => {
				val event = events.head
				TimetableEvent(
					event.name,
					event.title,
					event.description,
					event.eventType,
					events.flatMap { _.weekRanges },
					event.day,
					event.startTime,
					event.endTime,
					event.location,
					event.context,
					event.staffUniversityIds,
					event.year
				)
				event
			}
		}.toList
	}

	def parseICal(is: InputStream): Seq[TimetableEvent] = {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true)

		val builder = new CalendarBuilder
		val cal = builder.build(is)

		cal.getComponents(Component.VEVENT).asScala.collect { case event: VEvent => event }.map { event =>
			val eventType =
				Option(event.getProperty(Property.CATEGORIES)).collect { case c: Categories if c.getCategories.size() == 1 => c }.map { c =>
					TimetableEventType(c.getCategories.iterator().next().asInstanceOf[String])
				}.orNull

			// Convert date/time to academic year, local times and week number
			val start = toDateTime(event.getStartDate)
			val end = toDateTime(event.getEndDate) match {
				case null => start
				case date => date
			}

			val day = DayOfWeek(start.getDayOfWeek)

			// Guess the year based on the term start date, not the actual date, to get around the comment in AcademicYear.guessByDate
			val year = AcademicYear.guessByDate(termService.getTermFromDateIncludingVacations(start).getStartDate)

			// Convert the date to an academic week number
			val startWeek = termService.getAcademicWeekForAcademicYear(start, year)

			// We support exactly one RRule of frequence weekly
			val endWeek = event.getProperties(Property.RRULE).asScala.headOption.collect {
				case rule: RRule if rule.getRecur.getFrequency == "WEEKLY" => rule
			}.map { rule =>
				startWeek + rule.getRecur.getCount - 1
			}.getOrElse(startWeek)

			val weekRange = WeekRange(startWeek, endWeek)

			val summary = Option(event.getSummary).fold("") { _.getValue }
			val moduleCode = summary.maybeText.collect { case r"([A-Za-z]{2}[0-9]{3})${m}.*" => m.toUpperCase() }

			TimetableEvent(
				name = summary,
				title = "",
				description = Option(event.getDescription).fold("") { _.getValue },
				eventType = eventType,
				weekRanges = Seq(weekRange),
				day = day,
				startTime = start.toLocalTime,
				endTime = end.toLocalTime,
				location = Option(event.getLocation).flatMap { _.getValue.maybeText },
				context = moduleCode,
				staffUniversityIds = Nil,
				year = year
			)
		}
	}

	// Doesn't support all-day events
	def toDateTime(property: DateProperty) =
		if (property == null) null
		else new DateTime(property.getDate, getTimeZone(property)).withZoneRetainFields(DateTimeZone.forID("Europe/London"))

	def getTimeZone(property: DateProperty) =
		if (property.getParameter(Parameter.VALUE) != null && (property.getParameter(Parameter.VALUE) == Value.DATE)) DateTimeZone.UTC
		else if (property.getTimeZone != null) DateTimeZone.forTimeZone(property.getTimeZone)
		else DateTimeZone.forID("Europe/London")
}