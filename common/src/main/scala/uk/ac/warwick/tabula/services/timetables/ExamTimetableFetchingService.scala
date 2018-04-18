package uk.ac.warwick.tabula.services.timetables

import org.apache.commons.codec.digest.DigestUtils
import org.apache.http.client.ResponseHandler
import org.apache.http.client.methods.HttpGet
import org.joda.time.format.{DateTimeFormat, DateTimeFormatter, PeriodFormatter, PeriodFormatterBuilder}
import org.joda.time.{DateTime, Period}
import org.springframework.stereotype.Service
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula._
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.ExecutionContexts.timetable
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.helpers.{ApacheHttpClientUtils, Logging}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.ExamTimetableFetchingService.ExamTimetable
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.concurrent.Future
import scala.util.Try
import scala.xml.Elem

trait ExamTimetableConfiguration {
	val examTimetableUrl: String
}

trait ExamTimetableConfigurationComponent {
	val examTimetableConfiguration: ExamTimetableConfiguration
}

trait AutowiringExamTimetableConfigurationComponent extends ExamTimetableConfigurationComponent {
	val examTimetableConfiguration = new AutowiringExamTimetableConfiguration

	class AutowiringExamTimetableConfiguration extends ExamTimetableConfiguration {
		lazy val examTimetableUrl: String = Wire.optionProperty("${examTimetable.url}").getOrElse("https://exams.warwick.ac.uk/timetable/")
	}

}

trait ExamTimetableHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: ExamTimetableConfigurationComponent =>

	lazy val timetableFetchingService = ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
}

private class ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration: ExamTimetableConfiguration)
	extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {

	self: ApacheHttpClientComponent with TrustedApplicationsManagerComponent
		with UserLookupComponent with FeaturesComponent with TopLevelUrlComponent =>

	// an HTTPClient response handler which reads XML from the response and parses it into a list of TimetableEvents
	def handler(uniId: String): ResponseHandler[Seq[TimetableEvent]] =
		ApacheHttpClientUtils.xmlResponseHandler { node =>
			ExamTimetableHttpTimetableFetchingService.parseXml(node, uniId, toplevelUrl)
		}

	private def featureProtected(arg: String)(f: (String) => Future[EventList]): Future[EventList] = {
		if (features.personalExamTimetables) {
			f(arg)
		} else {
			Future(EventList(Nil, None))
		}
	}


	override def getTimetableForStudent(universityId: String): Future[EventList] = featureProtected(universityId)(doRequest)
	override def getTimetableForStaff(universityId: String): Future[EventList] = featureProtected(universityId)(doRequest)

	def doRequest(param: String): Future[EventList] = {
		userLookup.getUserByWarwickUniId(param) match {
			case user if !user.isFoundUser =>
				logger.warn(s"Tried to get exam timetable for $param but could find no such user")
				Future.successful(EventList(Nil, None))
			case user =>
				val endpoint = s"${examTimetableConfiguration.examTimetableUrl}timetable.xml"

				val req = new HttpGet(endpoint)
				TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, user.getUserId, req)

				logger.info(s"Requesting exam timetable data from $endpoint")

				val result = Future { httpClient.execute(req, handler(param)) }
				result.onFailure { case e =>
					logger.warn(s"Request for $endpoint failed: ${e.getMessage}")
				}
				result.map(EventList.fresh)
		}
	}

}

object ExamTimetableHttpTimetableFetchingService extends Logging {

	val cacheName = "ExamTimetableLists"

	def apply(examTimetableConfiguration: ExamTimetableConfiguration): CachedStaffAndStudentTimetableFetchingService = {
		val service =	new ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
			with AutowiringApacheHttpClientComponent
			with AutowiringTrustedApplicationsManagerComponent
			with AutowiringUserLookupComponent
			with AutowiringFeaturesComponent
			with AutowiringTopLevelUrlComponent

		new CachedStaffAndStudentTimetableFetchingService(service, cacheName)
	}

	def parseXml(xml: Elem, universityId: String, topLevelUrl: String): Seq[TimetableEvent] = {
		val timetable = ExamTimetableFetchingService.examTimetableFromXml(xml)
		val examProfile = (xml \\ "exam-profile" \\ "profile").text

		timetable.exams.map(timetableExam => {
			val uid = DigestUtils.md5Hex(Seq(examProfile, timetableExam.paper).mkString)
			TimetableEvent(
				uid = uid,
				name = "Exam",
				title = "Exam",
				description = "",
				eventType = TimetableEventType.Exam,
				weekRanges = Seq(WeekRange(timetableExam.academicYear.weekForDate(timetableExam.startDateTime.toLocalDate).weekNumber)),
				day = DayOfWeek(timetableExam.startDateTime.dayOfWeek.get),
				startTime = timetableExam.startDateTime.toLocalTime,
				endTime = timetableExam.endDateTime.toLocalTime,
				location = None,
				comments = Some(s"""More information available on the <a href="$topLevelUrl${Routes.Profile.examTimetable(universityId)}">exam timetable</a>"""),
				parent = TimetableEvent.Parent(),
				staff = Nil,
				students = Nil,
				year = timetableExam.academicYear,
				relatedUrl = None,
				attendance = Map()
			)
		})
	}
}

object ExamTimetableFetchingService {

	val examDateTimeFormatter: DateTimeFormatter = DateTimeFormat.forPattern("dd MMM HH:mm")
	val examPeriodFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix("hr ").appendMinutes().appendSuffix("mins").toFormatter
	val examReadingTimeFormatter: PeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter

	case class ExamTimetableExam(
		academicYear: AcademicYear,
		moduleCode: String,
		paper: String,
		section: String,
		length: Period,
		lengthString: String,
		readingTime: Option[Period],
		isOpenBook: Boolean,
		startDateTime: DateTime,
		endDateTime: DateTime,
		extraTimePerHour: Option[Int],
		room: String
	)

	case class ExamTimetable(
		header: String,
		instructions: String,
		exams: Seq[ExamTimetableExam]
	)

	def examTimetableFromXml(xml: Elem): ExamTimetable = {
		val header = (xml \\ "exam-headerinfo").text
		val instructions = (xml \\ "exam-instructions").text

		if ((xml \\ "exam").isEmpty) {
			ExamTimetable(header, instructions, Nil)
		} else {
			val academicYear = AcademicYear((xml \\ "academic-year").text.toInt).extended
			val exams = (xml \\ "exam").map(examNode => {
				val moduleCode = (examNode \\ "module").text
				val paper = (examNode \\ "paper").text
				val section = (examNode \\ "section").text
				val lengthString = (examNode \\ "length").text
				val length = examPeriodFormatter.parsePeriod(lengthString)
				val readingTime = (examNode \\ "read-time").headOption.flatMap(readingTimeNode =>
					readingTimeNode.text.maybeText.map(s => examReadingTimeFormatter.parsePeriod(s.replace("01/01/1900 ", "")))
				)
				val isOpenBook = (examNode \\ "open-book").text.maybeText.isDefined

				val startDateTime = examDateTimeFormatter.parseDateTime("%s %s".format((examNode \\ "date").text, (examNode \\ "time").text)).withYear(academicYear.endYear)
				val extraTimePerHour = (examNode \\ "extratime-perhr").headOption.flatMap(extraTimePerHourNode =>
					extraTimePerHourNode.text.maybeText.flatMap(s => Try(s.toInt).toOption)
				)
				val extraTime = extraTimePerHour.map(minutes =>
					Period.minutes((length.toStandardMinutes.getMinutes.toFloat / 60 * minutes).toInt)
				)
				val endDateTime = Seq(Some(length), readingTime, extraTime).flatten.foldLeft(startDateTime)((dt, period) => dt.plus(period))

				val room = (examNode \\ "room").text

				ExamTimetableExam(
					academicYear,
					moduleCode,
					paper,
					section,
					length,
					lengthString,
					readingTime,
					isOpenBook,
					startDateTime,
					endDateTime,
					extraTimePerHour,
					room
				)
			})
			ExamTimetable(header, instructions, exams)
		}
	}

}

trait ExamTimetableFetchingService {
	def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable]
}

abstract class AbstractExamTimetableFetchingService extends ExamTimetableFetchingService with Logging {

	self: ApacheHttpClientComponent with ExamTimetableConfigurationComponent
		with TrustedApplicationsManagerComponent =>

	def getTimetable(member: Member, viewer: CurrentUser): Future[ExamTimetableFetchingService.ExamTimetable] = {
		val endpoint = s"${examTimetableConfiguration.examTimetableUrl}${member.universityId}.xml"

		val req = new HttpGet(endpoint)
		TrustedApplicationUtils.signRequest(applicationManager.getCurrentApplication, viewer.userId, req)

		logger.info(s"Requesting exam timetable data from $endpoint")

		// an HTTPClient response handler which reads XML from the response and parses it into an ExamTimetable
		val handler: ResponseHandler[ExamTimetable] =
			ApacheHttpClientUtils.xmlResponseHandler { node =>
				ExamTimetableFetchingService.examTimetableFromXml(node)
			}

		val result = Future { httpClient.execute(req, handler) }
		result.onFailure { case e =>
			logger.warn(s"Request for $endpoint failed: ${e.getMessage}")
		}
		result
	}
}

@Service("examTimetableFetchingService")
class ExamTimetableFetchingServiceImpl
	extends AbstractExamTimetableFetchingService
	with AutowiringApacheHttpClientComponent
	with AutowiringExamTimetableConfigurationComponent
	with AutowiringTrustedApplicationsManagerComponent
	with AutowiringUserLookupComponent

trait ExamTimetableFetchingServiceComponent {
	def examTimetableFetchingService: ExamTimetableFetchingService
}

trait AutowiringExamTimetableFetchingServiceComponent extends ExamTimetableFetchingServiceComponent {
	var examTimetableFetchingService: ExamTimetableFetchingService = Wire[ExamTimetableFetchingService]
}