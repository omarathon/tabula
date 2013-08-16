package uk.ac.warwick.tabula.profiles.services.timetables

import org.springframework.beans.factory.DisposableBean
import org.springframework.stereotype.Service
import dispatch.classic._
import dispatch.classic.Request.toRequestVerbs
import uk.ac.warwick.tabula.helpers.Logging
import org.apache.http.client.params.ClientPNames
import org.apache.http.client.params.CookiePolicy
import dispatch.classic.thread.ThreadSafeHttpClient
import scala.xml.Elem
import org.joda.time.LocalTime
import uk.ac.warwick.tabula.data.model.groups._
import scala.Some
import uk.ac.warwick.tabula.data.model.groups.SmallGroupFormat._
import scala.Some
import uk.ac.warwick.spring.Wire

trait TimetableFetchingServiceComponent {
	def timetableFetchingService:TimetableFetchingService

	trait TimetableFetchingService {
		def getTimetableForStudent(universityId: String): Seq[TimetableEvent]
		def getTimetableForModule(moduleCode: String): Seq[TimetableEvent]
		def getTimetableForCourse(courseCode: String): Seq[TimetableEvent]
		def getTimetableForRoom(roomName: String): Seq[TimetableEvent]
		def getTimetableForStaff(universityId: String): Seq[TimetableEvent]
	}
}
trait ScientiaConfigurationComponent{
	val scientiaConfiguration:ScientiaConfiguration
	trait ScientiaConfiguration{
		val baseUri:String
	}
}
trait AutowiringScientiaConfigurationComponent extends ScientiaConfigurationComponent{
	val scientiaConfiguration = new AutowiringScientiaConfiguration
	class AutowiringScientiaConfiguration extends ScientiaConfiguration{
		val baseUri:String =	Wire.optionProperty("${scientia.base.url}").getOrElse("https://test-timetablingmanagement.warwick.ac.uk/xml/")
	}
}

trait ScientiaHttpTimetableFetchingServiceComponent extends TimetableFetchingServiceComponent{

	this:ScientiaConfigurationComponent =>
	def timetableFetchingService = new ScientiaHttpTimetableFetchingService

	class ScientiaHttpTimetableFetchingService extends TimetableFetchingService with Logging with DisposableBean {
		import ScientiaHttpTimetableFetchingService._

		lazy val baseUri = scientiaConfiguration.baseUri

		lazy val studentUri = baseUri + "?StudentXML"
		lazy val staffUri = baseUri + "?StaffXML"
		lazy val courseUri = baseUri + "?CourseXML"
		lazy val moduleUri = baseUri + "?ModuleXML"
		lazy val roomUri = baseUri + "?RoomXML"

		val http: Http = new Http with thread.Safety {
			override def make_client = new ThreadSafeHttpClient(new Http.CurrentCredentials(None), maxConnections, maxConnectionsPerRoute) {
				getParams().setParameter(ClientPNames.COOKIE_POLICY, CookiePolicy.IGNORE_COOKIES)
			}
		}

		override def destroy {
			http.shutdown()
		}

		val handler = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
			req <> { (node) => parseXml(node) }
		}

		def getTimetableForStudent(universityId: String): Seq[TimetableEvent] = doRequest(studentUri, universityId)
		def getTimetableForModule(moduleCode: String): Seq[TimetableEvent] = doRequest(moduleUri, moduleCode)
		def getTimetableForCourse(courseCode: String): Seq[TimetableEvent] = doRequest(courseUri, courseCode)
		def getTimetableForRoom(roomName: String): Seq[TimetableEvent] = doRequest(roomUri, roomName)
		def getTimetableForStaff(universityId: String): Seq[TimetableEvent] = doRequest(staffUri, universityId)

		def doRequest(uri: String, param: String) = {
			val req = url(uri) <<? Map("p0" -> param)
			http.x(req >:+ handler)
		}

	}
}
object ScientiaHttpTimetableFetchingService {
	
	def parseXml(xml: Elem): Seq[TimetableEvent] =
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
					case text if !text.isEmpty => Some(text)
					case _ => None
				},
				moduleCode = (activity \\ "module").text,
				staffUniversityIds = (activity \\ "staffmember") map { _.text }
			)
		}
	
}

//TODO extract this into it's own file, and put the EventOccurrence class with it.
case class TimetableEvent(
	name: String,
	description: String,
	eventType: TimetableEventType,
	weekRanges: Seq[WeekRange],
	day: DayOfWeek,
	startTime: LocalTime,
	endTime: LocalTime,
	location: Option[String],
	moduleCode: String,
	staffUniversityIds: Seq[String]
)

object TimetableEvent{
	def apply(sge:SmallGroupEvent):TimetableEvent = {
		TimetableEvent(name = sge.group.name,
			description = sge.group.groupSet.name,
			eventType = smallGroupFormatToTimetableEventType(sge.group.groupSet.format),
			weekRanges = sge.weekRanges,
			day = sge.day,
			startTime = sge.startTime,
			endTime = sge.endTime,
			location = Option(sge.location),
			moduleCode = sge.group.groupSet.module.code,
			sge.tutors.members)
	}
	private def smallGroupFormatToTimetableEventType(sgf: SmallGroupFormat): TimetableEventType = {
		sgf match {
			case Seminar => TimetableEventType.Seminar
			case Lab => TimetableEventType.Practical
			case Tutorial => TimetableEventType.Other("Tutorial")
			case Project => TimetableEventType.Other("Project")
			case Example => TimetableEventType.Other("Example")
		}
	}
}

sealed abstract class TimetableEventType(val code: String)
object TimetableEventType {
	case object Lecture extends TimetableEventType("LEC")
	case object Practical extends TimetableEventType("PRA")
	case object Seminar extends TimetableEventType("SEM")
	case object Induction extends TimetableEventType("IND")
	case class Other(c: String) extends TimetableEventType(c)
	
	// lame manual collection. Keep in sync with the case objects above
	val members = Seq(Lecture, Practical, Seminar, Induction)
	
	def unapply(code: String): Option[TimetableEventType] = code match {
		case Lecture.code => Some(Lecture)
		case Practical.code => Some(Practical)
		case Seminar.code => Some(Seminar)
		case Induction.code => Some(Induction)
		case _ => None
	}
	
	def apply(code: String): TimetableEventType = code match {
		case TimetableEventType(t) => t
		case _ => Other(code)
	}
}