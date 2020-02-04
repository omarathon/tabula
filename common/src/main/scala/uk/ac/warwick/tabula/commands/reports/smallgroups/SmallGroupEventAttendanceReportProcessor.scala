package uk.ac.warwick.tabula.commands.reports.smallgroups

import org.joda.time.LocalDate
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.smallgroups.SmallGroupEventAttendanceReportProcessor.Result
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandState, ReportPermissions, ReportsDateFormats}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.groups.DayOfWeek

import scala.jdk.CollectionConverters._

object SmallGroupEventAttendanceReportProcessor {
  type Result = Seq[SmallGroupEventAttendanceReportProcessorResult]
  type Command = Appliable[Result] with SmallGroupEventAttendanceReportProcessorRequest

  def apply(department: Department, academicYear: AcademicYear): Command =
    new SmallGroupEventAttendanceReportProcessorInternal(department, academicYear)
      with ComposableCommand[Result]
      with SmallGroupEventAttendanceReportProcessorRequest
      with ReportPermissions
      with Unaudited with ReadOnly {
      // Needed for task benchmarking
      override lazy val eventName: String = "SmallGroupEventAttendanceReportProcessor"
    }
}

case class SmallGroupEventAttendanceReportProcessorResult(
  id: String,
  moduleCode: String,
  setName: String,
  format: String,
  groupName: String,
  week: Int,
  day: Int,
  dayString: String,
  eventDate: LocalDate,
  location: String,
  tutors: String,
  isLate: Boolean,
  recorded: Int,
  unrecorded: Int,
  earliestRecordedAttendance: Option[LocalDate],
  latestRecordedAttendance: Option[LocalDate]
)

abstract class SmallGroupEventAttendanceReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[Result]
    with ReportCommandState {
  self: SmallGroupEventAttendanceReportProcessorRequest =>

  override def applyInternal(): Result =
    events.asScala.toSeq.map { properties =>
      SmallGroupEventAttendanceReportProcessorResult(
        id = properties.get("id"),
        moduleCode = properties.get("moduleCode"),
        setName = properties.get("setName"),
        format = properties.get("format"),
        groupName = properties.get("groupName"),
        week = properties.get("week").toInt,
        day = properties.get("day").toInt,
        dayString = DayOfWeek(properties.get("day").toInt).getName,
        eventDate = ReportsDateFormats.ReportDate.parseLocalDate(properties.get("eventDate")),
        location = properties.get("location"),
        tutors = properties.get("tutors"),
        isLate = properties.get("late").toBoolean,
        recorded = properties.get("recorded").toInt,
        unrecorded = properties.get("unrecorded").toInt,
        earliestRecordedAttendance = Option(properties.get("earliestRecordedAttendance")).map(ReportsDateFormats.ReportDate.parseLocalDate),
        latestRecordedAttendance = Option(properties.get("latestRecordedAttendance")).map(ReportsDateFormats.ReportDate.parseLocalDate)
      )
    }.sortBy(event => (event.eventDate, event.moduleCode, event.setName, event.groupName))
}

trait SmallGroupEventAttendanceReportProcessorRequest extends ReportCommandRequest {
  self: ReportCommandState =>

  var events: JList[JMap[String, String]] = JArrayList()
}
