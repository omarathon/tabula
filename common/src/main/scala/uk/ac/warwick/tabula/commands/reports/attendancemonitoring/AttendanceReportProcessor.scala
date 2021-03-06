package uk.ac.warwick.tabula.commands.reports.attendancemonitoring

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.reports.{ReportCommandRequest, ReportCommandState, ReportPermissions, ReportsDateFormats}
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}

import scala.jdk.CollectionConverters._

object AttendanceReportProcessor {
  def apply(department: Department, academicYear: AcademicYear) =
    new AttendanceReportProcessorInternal(department, academicYear)
      with ComposableCommand[AttendanceReportProcessorResult]
      with AutowiringProfileServiceComponent
      with ReportPermissions
      with AttendanceReportProcessorState
      with ReadOnly with Unaudited {
      override lazy val eventName: String = "AttendanceReportProcessor"
    }
}

case class PointData(
  id: String,
  name: String,
  startDate: LocalDate,
  endDate: LocalDate,
  isLate: Boolean
)

case class AttendanceReportProcessorResult(
  attendance: Map[AttendanceMonitoringStudentData, Map[PointData, AttendanceState]],
  students: Seq[AttendanceMonitoringStudentData],
  points: Seq[PointData],
  reportRangeStartDate: String,
  reportRangeEndDate: String
)

class AttendanceReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
  extends CommandInternal[AttendanceReportProcessorResult] with TaskBenchmarking {

  self: AttendanceReportProcessorState with ProfileServiceComponent =>

  override def applyInternal(): AttendanceReportProcessorResult = {
    val processedStudents = students.asScala.toSeq.map { properties =>
      AttendanceMonitoringStudentData(
        properties.get("firstName"),
        properties.get("lastName"),
        properties.get("universityId"),
        null,
        null,
        null,
        properties.get("route"),
        null,
        properties.get("yearOfStudy"),
        properties.get("sprCode"),
        tier4Requirements = properties.get("tier4Requirements").toBoolean,
        properties.get("email"),
        Option(properties.get("tutorEmail"))
      )
    }.sortBy(s => (s.lastName, s.firstName))
    import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
    val processedPoints = points.asScala.toSeq.map { properties =>
      PointData(
        properties.get("id"),
        properties.get("name"),
        new LocalDate(properties.get("startDate").toLong),
        new LocalDate(properties.get("endDate").toLong),
        new DateTime(properties.get("endDate").toLong).plusDays(1).isBeforeNow
      )
    }.sortBy(p => (p.startDate, p.endDate))
    val processedAttendance = attendance.asScala.flatMap { case (universityId, pointMap) =>
      processedStudents.find(_.universityId == universityId).map(studentData =>
        studentData -> pointMap.asScala.flatMap { case (id, stateString) =>
          processedPoints.find(_.id == id).map(point => point -> AttendanceState.fromCode(stateString))
        }.toMap)
    }.toMap
    AttendanceReportProcessorResult(processedAttendance, processedStudents, processedPoints,
      ReportsDateFormats.CSVDate.print(reportRangeStartDate), ReportsDateFormats.CSVDate.print(reportRangeEndDate))
  }

}

trait AttendanceReportProcessorState extends ReportCommandState with ReportCommandRequest {
  var attendance: JMap[String, JMap[String, String]] =
    LazyMaps.create { _: String => JMap[String, String]() }.asJava

  var students: JList[JMap[String, String]] = JArrayList()

  var points: JList[JMap[String, String]] = JArrayList()

  var reportRangeStartDate: LocalDate = _
  var reportRangeEndDate: LocalDate = _
}
