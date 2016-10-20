package uk.ac.warwick.tabula.services.timetables

import java.io.InputStream

import dispatch.classic._
import net.fortuna.ical4j.data.CalendarBuilder
import net.fortuna.ical4j.model.component.VEvent
import net.fortuna.ical4j.model.parameter.Value
import net.fortuna.ical4j.model.property.{Categories, DateProperty, RRule}
import net.fortuna.ical4j.model.{Component, Parameter, Property}
import net.fortuna.ical4j.util.CompatibilityHints
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.format.DateTimeFormat
import org.joda.time.{DateTime, DateTimeZone}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{FoundUser, Logging}
import uk.ac.warwick.tabula.services.UserLookupService.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.permissions.{AutowiringCacheStrategyComponent, CacheStrategyComponent}
import uk.ac.warwick.tabula.services.timetables.CelcatHttpTimetableFetchingService._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}
import uk.ac.warwick.tabula.{AcademicYear, AutowiringFeaturesComponent}
import uk.ac.warwick.userlookup.UserLookupException
import uk.ac.warwick.util.cache.{CacheEntryUpdateException, Caches, SingularCacheEntryFactory}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.util.parsing.json.JSON
import scala.util.{Failure, Success, Try}

sealed abstract class FilenameGenerationStrategy
object FilenameGenerationStrategy {
	case object Default extends FilenameGenerationStrategy
	case object BSV extends FilenameGenerationStrategy
	case object WBS extends FilenameGenerationStrategy
}

trait CelcatConfiguration {
	val departmentConfiguration: Map[String, CelcatDepartmentConfiguration]
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
	parseWBSJson: Boolean = false,
	enabledFn: () => Boolean = { () => true },
	credentials: Credentials
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
				parseWBSJson = false,
				enabledFn = { () => features.celcatTimetablesChemistry },
				credentials = Credentials(Wire.property("${celcat.fetcher.ch.username}"), Wire.property("${celcat.fetcher.ch.password}"))
			),
			"ib" -> CelcatDepartmentConfiguration(
				baseUri = "https://rimmer.wbs.ac.uk",
				staffFilenameLookupStrategy = FilenameGenerationStrategy.WBS,
				staffListInBSV = false,
				parseWBSJson = true,
				enabledFn = { () => features.celcatTimetablesWBS },
				credentials = Credentials(Wire.property("${celcat.fetcher.ib.username}"), Wire.property("${celcat.fetcher.ib.password}"))
			)
		)

		val cacheEnabled = true
	}
}

trait CelcatHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: CelcatConfigurationComponent =>

	lazy val timetableFetchingService = CelcatHttpTimetableFetchingService(celcatConfiguration)
}

object CelcatHttpTimetableFetchingService {
	val cacheName = "CelcatTimetableLists"
	val expectedModuleCodeLength = 5

	def apply(celcatConfiguration: CelcatConfiguration): StudentTimetableFetchingService with StaffTimetableFetchingService = {
		val delegate =
			new CelcatHttpTimetableFetchingService(celcatConfiguration)
				with AutowiringUserLookupComponent
				with AutowiringTermServiceComponent
				with AutowiringCacheStrategyComponent
				with WAI2GoHttpLocationFetchingServiceComponent
				with AutowiringWAI2GoConfigurationComponent
				with AutowiringModuleAndDepartmentServiceComponent
				with AutowiringDispatchHttpClientComponent

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
		locationFetchingService: LocationFetchingService,
		moduleMap: Map[String, Module],
		userLookup: UserLookupService
	)(implicit termService: TermService): Option[TimetableEvent] = {
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

			val year = AcademicYear.findAcademicYearContainingDate(start)

			// Convert the date to an academic week number
			val startWeek = termService.getAcademicWeekForAcademicYear(start, year)

			// We support exactly one RRule of frequence weekly
			val endWeek = event.getProperties(Property.RRULE).asScala.headOption.collect {
				case rule: RRule if rule.getRecur.getFrequency == "WEEKLY" => rule
			}.map { rule =>
				startWeek + rule.getRecur.getCount - 1
			}.getOrElse(startWeek)

			val weekRange = WeekRange(startWeek, endWeek)

			// From the Celcat iCal files we get SUMMARY in the format of
			// (ROOM) - STAFF1/STAFF2 (WEEK_START-WEEK_END)/STAFF3 etc - all optional. Do nasty format parsing
			val staffIds: Seq[UniversityId] =
				if (allStaff.nonEmpty)
					summary.maybeText
						.filter { _.contains(" - ") } // Quick exit - only look for summaries in the right format
						.map { _.split(" - ", 2).last } // Strip the "room" from the start
						.map {
							// Split on / to get a list of names and their optional week numbers
							_.split('/').toSeq
								// Strip the week numbers off the end
								.collect { case r"([^/]+?)${nameOrInitial}(?: (?:[0-9\\-]+,?)+)?" => nameOrInitial }
						}
						.map { namesOrInitials =>
							// Match up against the staff BSV fetched separately
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
				year = year,
				relatedUrl = None
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

class CelcatHttpTimetableFetchingService(celcatConfiguration: CelcatConfiguration) extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {
	self: UserLookupComponent
		with TermServiceComponent
		with LocationFetchingServiceComponent
		with CacheStrategyComponent
		with ModuleAndDepartmentServiceComponent
		with DispatchHttpClientComponent =>

	lazy val configs = celcatConfiguration.departmentConfiguration

	// a dispatch response handler which reads iCal/JSON from the response and parses it into a list of TimetableEvents
	def handler(config: CelcatDepartmentConfiguration) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		if (config.parseWBSJson) {
			req >- { (rawJSON) =>
				combineIdenticalEvents(parseJSON(rawJSON))
			}
		}
		else {
			req >> { (is, json) => {
				combineIdenticalEvents(parseICal(is, config))
			}}
		}
	}

	def getTimetableForStudent(universityId: UniversityId): Future[EventList] = {
		userLookup.getUserByWarwickUniId(universityId) match {
			case FoundUser(u) if u.getDepartmentCode.hasText =>
				configs.get(u.getDepartmentCode.toLowerCase).filter(_.enabled).map { config =>
					val filename = {
						if (config.parseWBSJson) s"${u.getWarwickId}"
						else s"${u.getWarwickId}.ics"
					}
					doRequest(filename, config)
				}.getOrElse(Future.successful(EventList.fresh(Nil)))

			case FoundUser(u) =>
				logger.warn(s"Found user for ${u.getWarwickId}, but not departmentCode. Returning empty Celcat timetable")
				Future.successful(EventList.fresh(Nil))
			case _ => Future.failed(new UserLookupException(s"No user found for university ID $universityId"))
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

	def getTimetableForStaff(universityId: UniversityId): Future[EventList] = {
		findConfigForStaff(universityId).map { config =>
			val filename = config.staffFilenameLookupStrategy match {
				case FilenameGenerationStrategy.Default => s"$universityId.ics"
				case FilenameGenerationStrategy.WBS => s"$universityId"
				case FilenameGenerationStrategy.BSV => lookupCelcatIDFromBSV(universityId, config).map { id => s"$id.ics" }.getOrElse(s"$universityId.ics")
			}

			doRequest(filename, config)
		}.getOrElse(Future.successful(EventList.fresh(Nil)))
	}

	type BSVCacheEntry = Seq[(UniversityId, CelcatStaffInfo)] with java.io.Serializable
	val bsvCacheEntryFactory = new SingularCacheEntryFactory[String, BSVCacheEntry] {
		def create(baseUri: String) = {
			val config = configs.filter { case (_, deptConfig) => deptConfig.baseUri == baseUri }
				.map { case (_, deptConfig) => deptConfig }
				.headOption.getOrElse(throw new IllegalArgumentException(s"No such config with baseUri $baseUri"))

			val req =
				(url(baseUri) / "staff.bsv" <<? Map("forcebasic" -> "true"))
					.as_!(config.credentials.username, config.credentials.password)

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
			Try(httpClient.when(_==200)(req >:+ bsvHandler)) match {
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

	def doRequest(filename: String, config: CelcatDepartmentConfiguration): Future[EventList] = {
		// Add {universityId}.ics to the URL
		val req =
			(url(config.baseUri) / filename <<? Map("forcebasic" -> "true"))
				.as_!(config.credentials.username, config.credentials.password)

		// Execute the request
		// If the status is OK, pass the response to the handler function for turning into TimetableEvents
		// else return an empty list.
		logger.info(s"Requesting timetable data from ${req.to_uri.toString}")
		val result =
			Future(httpClient.when(_==200)(req >:+ handler(config)))
				.recover { case StatusCode(404, _) =>
					// Special case a 404, just return no events
					logger.warn(s"Request for ${req.to_uri.toString} returned a 404")
					EventList.fresh(Nil)
				}

		// Extra logging
		result.onFailure { case e =>
			logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
		}

		result
	}

	def combineIdenticalEvents(events: EventList): EventList = events.map { events =>
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
					event.year,
					event.relatedUrl
				)
		}}.toList
	}

	def parseJSON(incomingJson: String) = {
		JSON.parseFull(incomingJson) match {
					case Some(jsonData: List[Map[String, Any]]@unchecked) =>
						EventList.fresh(jsonData.flatMap { event =>
							val start = DateTime.parse(event.getOrElse("start", "").toString, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
							val end = DateTime.parse(event.getOrElse("end","").toString, DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ss'Z'"))
							val year = AcademicYear.findAcademicYearContainingDate(start)
							val moduleCode = event.getOrElse("moduleCode","").toString
							val module = moduleAndDepartmentService.getModuleByCode(moduleCode.toLowerCase.safeSubstring(0, expectedModuleCodeLength))
							val parent = TimetableEvent.Parent(module)
							val room = event.getOrElse("room","").toString
							val location = Option(locationFetchingService.locationFor(room))
							val eventType:TimetableEventType = event("contactType") match {
								case "L" => TimetableEventType.Lecture
								case "S" => TimetableEventType.Seminar
								case _ => TimetableEventType.Other("")
							}
							val uid =
								DigestUtils.md5Hex(
									Seq(
										module,
										start.toString,
										end.toString,
										location.fold("") {_.name},
										parent.shortName.getOrElse("")
									).mkString
								)

							Seq(TimetableEvent(
								uid = uid,
								name = moduleCode,
								title = "",
								description = "",
								eventType = eventType,
								weekRanges =  Seq(WeekRange(termService.getAcademicWeekForAcademicYear(start, year))),
								day = DayOfWeek(start.getDayOfWeek),
								startTime = start.toLocalTime,
								endTime = end.toLocalTime,
								location = location,
								comments = None,
								parent = parent,
								staff = Nil,
								students = Nil,
								year = AcademicYear.guessSITSAcademicYearByDate(start),
								relatedUrl = None
							))
						})
					case _ => throw new RuntimeException("Could not parse JSON")
				}
	}

	def parseICal(is: InputStream, config: CelcatDepartmentConfiguration)(implicit termService: TermService): EventList = {
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_PARSING, true)
		CompatibilityHints.setHintEnabled(CompatibilityHints.KEY_RELAXED_VALIDATION, true)

		val builder = new CalendarBuilder
		val cal = builder.build(is)

		val allStaff = staffInfo(config)

		val vEvents = cal.getComponents(Component.VEVENT).asScala.collect { case event: VEvent => event }
		val moduleMap = moduleAndDepartmentService.getModulesByCodes(
			vEvents.flatMap(e => parseModuleCode(e).map(_.toLowerCase)).distinct
		).groupBy(_.code).mapValues(_.head)

		EventList.fresh(vEvents.flatMap { event =>
			parseVEvent(event, allStaff, config, locationFetchingService, moduleMap, userLookup)
		})
	}
}

@SerialVersionUID(5445676324342l) case class CelcatStaffInfo(celcatId: String, universityId: UniversityId, initials: String, fullName: String) extends Serializable