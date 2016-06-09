package uk.ac.warwick.tabula.services.timetables

import dispatch.classic.url
import org.apache.commons.codec.digest.DigestUtils
import org.joda.time.{DateTime, Period}
import org.joda.time.format.{DateTimeFormat, PeriodFormatterBuilder}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.sso.client.trusted.TrustedApplicationUtils
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.groups.{DayOfWeek, WeekRange}
import uk.ac.warwick.tabula.helpers.Futures._
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.timetables.TimetableFetchingService.EventList
import uk.ac.warwick.tabula.timetables.{TimetableEvent, TimetableEventType}

import scala.collection.JavaConverters._
import scala.concurrent.Future
import scala.xml.Elem
import uk.ac.warwick.tabula.helpers.StringUtils._

import scala.util.Try

trait ExamTimetableConfiguration {
	val examTimetableUrl: String
}

trait ExamTimetableConfigurationComponent {
	val examTimetableConfiguration: ExamTimetableConfiguration
}

trait AutowiringExamTimetableConfigurationComponent extends ExamTimetableConfigurationComponent {
	val examTimetableConfiguration = new AutowiringExamTimetableConfiguration

	class AutowiringExamTimetableConfiguration extends ExamTimetableConfiguration {
		lazy val examTimetableUrl = Wire.optionProperty("${examTimetable.url}").getOrElse("https://exams.warwick.ac.uk/timetable/")
	}

}

trait ExamTimetableHttpTimetableFetchingServiceComponent extends StaffAndStudentTimetableFetchingServiceComponent {
	self: ExamTimetableConfigurationComponent =>

	lazy val timetableFetchingService = ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
}

private class ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration: ExamTimetableConfiguration)
	extends StaffTimetableFetchingService with StudentTimetableFetchingService with Logging {

	self: DispatchHttpClientComponent with TrustedApplicationsManagerComponent
		with UserLookupComponent with TermServiceComponent =>

	// a dispatch response handler which reads XML from the response and parses it into a list of TimetableEvents
	def handler(uniId: String) = { (headers: Map[String,Seq[String]], req: dispatch.classic.Request) =>
		req <> { node =>
			ExamTimetableHttpTimetableFetchingService.parseXml(node, uniId, termService)
		}
	}


	override def getTimetableForStudent(universityId: String) = doRequest(universityId)
	override def getTimetableForStaff(universityId: String) = doRequest(universityId)

	def doRequest(param: String): Future[EventList] = {
		userLookup.getUserByWarwickUniId(param) match {
			case user if !user.isFoundUser =>
				logger.warn(s"Tried to get exam timetable for $param but could find no such user")
				Future(EventList(Nil, None))
			case user =>
				val endpoint = s"${examTimetableConfiguration.examTimetableUrl}$param.xml"

				val trustedAppHeaders = TrustedApplicationUtils.getRequestHeaders(
					applicationManager.getCurrentApplication,
					user.getUserId,
					endpoint
				).asScala.map { header => header.getName -> header.getValue }.toMap

				val req = url(endpoint) <:< trustedAppHeaders

				logger.info(s"Requesting exam timetable data from ${req.to_uri.toString}")

				val result = Future {
					httpClient.when(_ == 200)(req >:+ handler(param))
				}
				result.onFailure { case e =>
					logger.warn(s"Request for ${req.to_uri.toString} failed: ${e.getMessage}")
				}
				result.map(EventList.fresh)
		}
	}

}

object ExamTimetableHttpTimetableFetchingService extends Logging {

	val cacheName = "ExamTimetableLists"
	val examDateTimeFormatter = DateTimeFormat.forPattern("dd MMM HH:mm")
	val examPeriodFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix("hr ").appendMinutes().appendSuffix("mins").toFormatter
	val examReadingTimeFormatter = new PeriodFormatterBuilder().appendHours().appendSuffix(":").appendMinutes().appendSuffix(":").appendSeconds().toFormatter

	def apply(examTimetableConfiguration: ExamTimetableConfiguration) = {
		val service =	new ExamTimetableHttpTimetableFetchingService(examTimetableConfiguration)
			with AutowiringDispatchHttpClientComponent
			with AutowiringTrustedApplicationsManagerComponent
			with AutowiringUserLookupComponent
			with AutowiringTermServiceComponent

		new CachedStaffAndStudentTimetableFetchingService(service, cacheName)
	}

	def parseXml(xml: Elem, uniId: String, termService: TermService): Seq[TimetableEvent] = {
		val examProfile = (xml \\ "exam-profile" \\ "profile").text
		val academicYear = AcademicYear.findAcademicYearContainingDate(DateTime.now)(termService)

		(xml \\ "exam").map(examNode => {
			val paper = (examNode \\ "paper").text
			val startDateTime = examDateTimeFormatter.parseDateTime("%s %s".format((examNode \\ "date").text, (examNode \\ "time").text)).withYear(academicYear.endYear)
			val length = examPeriodFormatter.parsePeriod((examNode \\ "length").text)
			val readingTime = (examNode \\ "read-time").headOption.flatMap(readingTimeNode =>
				readingTimeNode.text.maybeText.map(s => examReadingTimeFormatter.parsePeriod(s.replace("01/01/1900 ", "")))
			)
			val extraTimePerHour = (examNode \\ "extratime-perhr").headOption.flatMap(extraTimePerHourNode =>
				extraTimePerHourNode.text.maybeText.flatMap(s =>
					Try(s.toInt).toOption
				).map(minutes =>
					Period.minutes((length.toStandardMinutes.getMinutes.toFloat / 60 * minutes).toInt)
				)
			)
			val endDateTime = Seq(Some(length), readingTime, extraTimePerHour).flatten.foldLeft(startDateTime)((dt, period) => dt.plus(period))
			val uid = DigestUtils.md5Hex(Seq(examProfile, paper).mkString)
			TimetableEvent(
				uid = uid,
				name = "Exam",
				title = "Exam",
				description = "Exam",
				eventType = TimetableEventType.Exam,
				weekRanges = Seq(WeekRange(termService.getTermFromDateIncludingVacations(startDateTime).getAcademicWeekNumber(startDateTime))),
				day = DayOfWeek(startDateTime.dayOfWeek.get),
				startTime = startDateTime.toLocalTime,
				endTime = endDateTime.toLocalTime,
				location = None,
				comments = None,
				parent = TimetableEvent.Parent(),
				staff = Nil,
				students = Nil,
				year = academicYear
			)
		})
	}
}