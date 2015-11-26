package uk.ac.warwick.tabula.services.timetables

import java.io.InputStream

import dispatch.classic._
import dispatch.classic.thread.ThreadSafeHttpClient
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.{Categories, DateProperty, RRule}
import net.fortuna.ical4j.model.{Component, Parameter, Property}
import net.fortuna.ical4j.util.CompatibilityHints
import org.apache.http.auth.AuthScope
import org.apache.http.client.params.{ClientPNames, CookiePolicy}
import org.apache.http.params.HttpConnectionParams
import org.joda.time.{DateTime, DateTimeZone}
import org.springframework.beans.factory.DisposableBean
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.services.timetables.CelcatHttpTimetableFetchingService._
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent, HttpClientDefaults}
import uk.ac.warwick.userlookup.UserLookupException
import uk.ac.warwick.util.cache.{CacheEntryUpdateException, Caches, SingularCacheEntryFactory}

import scala.collection.JavaConverters._
import scala.util.{Failure, Success, Try}

sealed abstract class FilenameGenerationStrategy
object FilenameGenerationStrategy {
	case object Default extends FilenameGenerationStrategy
	case object BSV extends FilenameGenerationStrategy
}

trait CelcatConfiguration {
	val departmentConfiguration: Map[String, CelcatDepartmentConfiguration]
	val authScope: AuthScope
	val credentials: Credentials
	val cacheEnabled: Boolean
}

trait CelcatConfigurationComponent {
	val celcatConfiguration: CelcatConfiguration
}

case class CelcatDepartmentConfiguration(
	baseUri: String,
	excludedEventTypes: Seq[TimetableEventType] = Nil,
	staffFilenameLookupStrategy: FilenameGenerationStrategy = FilenameGenerationStrategy.Default,
	staffListInBSV: Boolean = false,
	enabledFn: () => Boolean = { () => true }
) {
	def enabled = enabledFn()
}

trait AutowiringCelcatConfigurationComponent extends CelcatConfigurationComponent {
	val celcatConfiguration = new AutowiringCelcatConfiguration

	class AutowiringCelcatConfiguration extends CelcatConfiguration with AutowiringFeaturesComponent {
		val departmentConfiguration =	Map(
			"ch" -> CelcatDepartmentConfiguration(
				baseUri = "https://www2.warwick.ac.uk/appdata/chem-timetables",
				staffFilenameLookupStrategy = FilenameGenerationStrategy.BSV,
				staffListInBSV = true,
				enabledFn = { () => features.celcatTimetablesChemistry }
			)
		)

		lazy val authScope = new AuthScope("www2.warwick.ac.uk", 443)
		lazy val credentials = Credentials(Wire.property("${celcat.fetcher.username}"), Wire.property("${celcat.fetcher.password}"))
		val cacheEnabled = true
	}
}

trait CelcatHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: CelcatConfigurationComponent =>

	lazy val timetableFetchingService = CelcatHttpTimetableFetchingService(celcatConfiguration)
}

object CelcatHttpTimetableFetchingService {
	val cacheName = "CelcatTimetables"

	def apply(celcatConfiguration: CelcatConfiguration): StudentTimetableFetchingService with StaffTimetableFetchingService = {
		val delegate = new CelcatHttpTimetableFetchingService(celcatConfiguration) with AutowiringUserLookupComponent with AutowiringTermServiceComponent
			with AutowiringCacheStrategyComponent with WAI2GoHttpLocationFetchingServiceComponent with AutowiringWAI2GoConfigurationComponent with AutowiringModuleAndDepartmentServiceComponent

		if (celcatConfiguration.cacheEnabled) {
			new CachedStaffAndStudentTimetableFetchingService(delegate, cacheName)
		} else {
			delegate
		}
	}

	private def parseModuleCode(event: VEvent): Option[String] = {
		val summary = Option(event.getSummary).fold("") { _.getValue }
		summary.maybeText.collect { case r"([A-Za-z]{2}[0-9][0-9A-Za-z]{2})${m}.*" => m.toUpperCase }
	}

	def parseVEvent(
		event: VEvent,
		allStaff: Map[UniversityId, CelcatStaffInfo],
		config: CelcatDepartmentConfiguration,
		termService: TermService,
		locationFetchingService: LocationFetchingService,
		moduleMap: Map[String, Module],
		userLookup: UserLookupService
	): Option[TimetableEvent] = {
		val summary = Option(event.getSummary).fold("") { _.getValue }
		val categories =
			Option(event.getProperty(Property.CATEGORIES))
				.collect { case c: Categories => c }
				.map { c => c.getCategories.iterator().asScala.collect { case s: String => s }.filter { _.hasText }.toList }
				.getOrElse(Nil)

		val eventType = categories match {
			case singleCategory :: Nil => TimetableEventType(singleCategory)

			case cats if cats.exists { c => TimetableEventType(c).core } =>
				cats.find { c => TimetableEventType(c).core }.map { c => TimetableEventType(c) }.get

			case _ =>	summary.split(" - ", 2) match {
				case Array(t, staffInfo) => TimetableEventType(t)
				case Array(s) => TimetableEventType(s)
			}
		}

		if (config.excludedEventTypes.contains(eventType) || eventType.code.toLowerCase.contains("on tabula")) None
		else {
			// Convert date/time to academic year, local times and week number
			val start = toDateTime(event.getStartDate)
			val end = toDateTime(event.getEndDate) match {
				case null => start
				case date => date
			}

			val day = DayOfWeek(start.getDayOfWeek)

			val year = AcademicYear.findAcademicYearContainingDate(start, termService)

			// Convert the date to an academic week number
			val startWeek = termService.getAcademicWeekForAcademicYear(start, year)

			// We support exactly one RRule of frequence weekly
			val endWeek = event.getProperties(Property.RRULE).asScala.headOption.collect {
				case rule: RRule if rule.getRecur.getFrequency == "WEEKLY" => rule
			}.map { rule =>
				startWeek + rule.getRecur.getCount - 1
			}.getOrElse(startWeek)

			val weekRange = WeekRange(startWeek, endWeek)

			println(summary)
			val staffIds: Seq[UniversityId] =
				if (allStaff.nonEmpty)
					summary.maybeText
						.collect { case r"^.* - ((?:[^/0-9]+(?: (?:[0-9\\-]+,?)+)?/?)+)${namesOrInitials}" =>
							println(namesOrInitials)
							namesOrInitials.split('/')
								.toSeq
								.collect { case r"([^/0-9]+)${nameOrInitial}(?: (?:[0-9\\-]+,?)+)?" => nameOrInitial }
					}
					.map { namesOrInitials =>
						namesOrInitials.flatMap { nameOrInitial => allStaff.values.find { info =>
							info.fullName == nameOrInitial || info.initials == nameOrInitial
						}.map { _.universityId }}
					}.getOrElse(Nil)
				else Nil

			val staff = userLookup.getUsersByWarwickUniIds(staffIds).values.collect { case FoundUser(u) => u }.toSeq

			Some(TimetableEvent(
				uid = event.getUid.getValue,
				name = summary,
				"",
				description = Option(event.getDescription).map { _.getValue }.filter { _.hasText }.getOrElse(summary),
				eventType = eventType,
				weekRanges = Seq(weekRange),
				day = day,
				startTime = start.toLocalTime,
				endTime = end.toLocalTime,
				location = Option(event.getLocation).flatMap { _.getValue.maybeText }.map(locationFetchingService.locationFor),
				comments = None,
				parent = TimetableEvent.Parent(parseModuleCode(event).flatMap(code => moduleMap.get(code.toLowerCase))),
				staff = staff,
				students = Nil,
				year = year
			))
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

class CelcatHttpTimetableFetchingService(celcatConfiguration: CelcatConfiguration) extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging with DisposableBean {
	self: UserLookupComponent with TermServiceComponent with LocationFetchingServiceComponent with CacheStrategyComponent with ModuleAndDepartmentServiceComponent =>

	lazy val configs = celcatConfiguration.departmentConfiguration

	val http: Http = new Http with thread.Safety {
		override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(Some(celcatConfiguration.authScope, celcatConfiguration.credentials)), maxConnections, maxConnectionsPerRoute) {
			HttpConnectionParams.setConnectionTimeout(getParams, HttpClientDefaults.connectTimeout)
			HttpConnectionParams.setSoTimeout(getParams, HttpClientDefaults.socketTimeout)
			getParams.setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
		}
	}

	override def destroy() {
		http.shutdown()
	}

	// a dispatch response handler which reads iCal from the response and parses it into a list of TimetableEvents
	def handler(config: CelcatDepartmentConfiguration) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req >> { (is) => combineIdenticalEvents(parseICal(is, config)) }
	}

	def getTimetableForStudent(universityId: UniversityId): Try[Seq[TimetableEvent]] = {
		userLookup.getUserByWarwickUniId(universityId) match {
			case FoundUser(u) if u.getDepartmentCode.hasText =>
				configs.get(u.getDepartmentCode.toLowerCase).filter(_.enabled).map { config =>
					doRequest(s"${u.getWarwickId}.ics", config)
				}.getOrElse(Success(Nil))
			case FoundUser(u) =>
				logger.warn(s"Found user for ${u.getWarwickId}, but not departmentCode. Returning empty Celcat timetable")
				Success(Nil)
			case _ => Failure(new UserLookupException(s"No user found for university ID $universityId"))
		}
	}

	def findConfigForStaff(universityId: UniversityId): Option[CelcatDepartmentConfiguration] = {
		userLookup.getUserByWarwickUniId(universityId) match {
			// User in a department with a config
			case FoundUser(u) if u.getDepartmentCode.hasText && configs.contains(u.getDepartmentCode.toLowerCase) =>
				configs.get(u.getDepartmentCode.toLowerCase).filter(_.enabled)

			// Look for a BSV-style config that contains the user
			case FoundUser(u) =>
				configs.values.filter(_.enabled)
					.filter { _.staffFilenameLookupStrategy == FilenameGenerationStrategy.BSV }
					.find { lookupCelcatIDFromBSV(u.getWarwickId, _).isDefined }

			case _ => None
		}
	}

	def getTimetableForStaff(universityId: UniversityId): Try[Seq[TimetableEvent]] = {
		findConfigForStaff(universityId).map { config =>
			val filename = config.staffFilenameLookupStrategy match {
				case FilenameGenerationStrategy.Default => s"$universityId.ics"
				case FilenameGenerationStrategy.BSV => lookupCelcatIDFromBSV(universityId, config).map { id => s"$id.ics" }.getOrElse(s"$universityId.ics")
			}

			doRequest(filename, config)
		}.getOrElse(Success(Nil))
	}

	type BSVCacheEntry = Seq[(UniversityId, CelcatStaffInfo)] with java.io.Serializable
	val bsvCacheEntryFactory = new SingularCacheEntryFactory[String, BSVCacheEntry] {
		def create(baseUri: String) = {
			val req = url(baseUri) / "staff.bsv" <<? Map("forcebasic" -> "true")

			def bsvHandler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
				req >- { _.split('\n').flatMap { _.split("\\|", 4) match {
					case Array(celcatId, staffId, initials, name) => Some(staffId -> CelcatStaffInfo(celcatId.trim(), staffId.trim(), initials.trim(), name.trim()))
					case _ => None
				}}.toList }
			}

			def toBSVCacheEntry(seq: Seq[(UniversityId, CelcatStaffInfo)]): BSVCacheEntry = {
				seq match {
					// can't use "case v: BSVCacheEntry" because the type inference engine in 2.10 can't cope.
					case v: Seq[(UniversityId, CelcatStaffInfo)] with java.io.Serializable => v
					case _ => throw new RuntimeException("Unserializable collection returned from TimetableFetchingService")
				}
			}

			// Execute the request
			logger.info(s"Requesting staff information from $req")
			Try(http.when(_==200)(req >:+ bsvHandler)) match {
				case Success(ev) => toBSVCacheEntry(ev)
				case Failure(ex) => throw new CacheEntryUpdateException(ex)
			}
		}
		def shouldBeCached(response: BSVCacheEntry) = true
	}

	lazy val bsvCache =
		Caches.newCache("CelcatBSVCache", bsvCacheEntryFactory, 60 * 60 * 24, cacheStrategy)

	def lookupCelcatIDFromBSV(universityId: UniversityId, config: CelcatDepartmentConfiguration) =
		staffInfo(config).get(universityId).map { _.celcatId }

	def staffInfo(config: CelcatDepartmentConfiguration): Map[UniversityId, CelcatStaffInfo] =
		if (config.staffListInBSV) {
			try {
				bsvCache.get(config.baseUri).toMap
			} catch {
				case e: CacheEntryUpdateException =>
					logger.error("Couldn't fetch staff BSV file for " + config.baseUri, e)
					Map()
			}
		} else Map()

	def doRequest(filename: String, config: CelcatDepartmentConfiguration): Try[Seq[TimetableEvent]] = {
		// Add {universityId}.ics to the URL
		val req = url(config.baseUri) / filename <<? Map("forcebasic" -> "true")

		// Execute the request
		// If the status is OK, pass the response to the handler function for turning into TimetableEvents
		// else return an empty list.
		logger.info(s"Requesting timetable data from ${req.to_uri.toString}")
		val result = try {
			Success(http.when(_==200)(req >:+ handler(config)))
		}	catch {
			case StatusCode(404, _) =>
				// Special case a 404, just return no events
				logger.warn(s"Request for ${req.to_uri.toString} returned a 404")
				Success(Nil)
			case e: Throwable =>
				Failure(e)
		}

		// Extra logging
		result match {
			case Success(ev) =>
				if (ev.isEmpty) logger.info("Timetable request successful but no events returned")
			case Failure(e) =>
				logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
		}

		result
	}

	def combineIdenticalEvents(events: Seq[TimetableEvent]): Seq[TimetableEvent] = {
		// If we run an identical event in separate weeks, combine the weeks for them
		val groupedEvents = events.groupBy { event =>
			(event.name, event.title, event.description, event.eventType, event.day, event.startTime, event.endTime,
				event.location, event.parent.shortName, event.staff, event.students, event.year)
		}.values.toSeq

		groupedEvents.map { eventSeq => eventSeq.size match {
			case 1 =>
				eventSeq.head
			case _ =>
				val event = eventSeq.head
				TimetableEvent(
					event.uid,
					event.name,
					event.title,
					event.description,
					event.eventType,
					eventSeq.flatMap {
						_.weekRanges
					},
					event.day,
					event.startTime,
					event.endTime,
					event.location,
					event.parent,
					event.comments,
					event.staff,
					event.students,
					event.year
				)
		}}.toList
	}

	def parseICal(is: InputStream, config: CelcatDepartmentConfiguration): Seq[TimetableEvent] = {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true)

		val builder = new CalendarBuilder
		val cal = builder.build(is)

		val allStaff = staffInfo(config)

		val vEvents = cal.getComponents(Component.VEVENT).asScala.collect { case event: VEvent => event }
		val moduleMap = moduleAndDepartmentService.getModulesByCodes(
			vEvents.flatMap(e => parseModuleCode(e).map(_.toLowerCase)).distinct
		).groupBy(_.code).mapValues(_.head)
		vEvents.flatMap { event =>
			parseVEvent(event, allStaff, config, termService, locationFetchingService, moduleMap, userLookup)
		}
	}
}

@SerialVersionUID(5445676324342l) case class CelcatStaffInfo(celcatId: String, universityId: UniversityId, initials: String, fullName: String) extends Serializable