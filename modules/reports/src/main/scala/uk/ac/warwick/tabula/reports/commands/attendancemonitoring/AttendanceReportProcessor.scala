package uk.ac.warwick.tabula.reports.commands.attendancemonitoring

import org.joda.time.{DateTime, LocalDate}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.AttendanceMonitoringStudentData
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.data.model.attendance.AttendanceState
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.reports.commands.{ReportCommandState, ReportPermissions}

import scala.collection.JavaConverters._

object AttendanceReportProcessor {
	def apply(department: Department, academicYear: AcademicYear) =
		new AttendanceReportProcessorInternal(department, academicYear)
			with ComposableCommand[AttendanceReportProcessorResult]
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
	result: Map[AttendanceMonitoringStudentData, Map[PointData, AttendanceState]],
	students: Seq[AttendanceMonitoringStudentData],
	points: Seq[PointData]
)

class AttendanceReportProcessorInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[AttendanceReportProcessorResult] with TaskBenchmarking {

	self: AttendanceReportProcessorState =>

	override def applyInternal() = {
		val processedStudents = students.asScala.map{case(universityId, properties) =>
			AttendanceMonitoringStudentData(
				properties.get("firstName"),
				properties.get("lastName"),
				universityId,
				null,
				null
			)
		}.toSeq.sortBy(s => (s.lastName, s.firstName))
		import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
		val processedPoints = points.asScala.map{case(id, properties) =>
			PointData(
				id,
				properties.get("name"),
				new LocalDate(properties.get("startDate").toLong),
				new LocalDate(properties.get("endDate").toLong),
				new DateTime(properties.get("endDate").toLong).isBeforeNow
			)
		}.toSeq.sortBy(p => (p.startDate, p.endDate))
		val processedResult = result.asScala.flatMap{case(universityId, pointMap) =>
			processedStudents.find(_.universityId == universityId).map(studentData =>
				studentData -> pointMap.asScala.flatMap { case (id, stateString) =>
					processedPoints.find(_.id == id).map(point => point -> AttendanceState.fromCode(stateString))
			}.toMap)
		}.toMap
		AttendanceReportProcessorResult(processedResult, processedStudents, processedPoints)
	}

}

trait AttendanceReportProcessorState extends ReportCommandState {
	var result: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var students: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava

	var points: JMap[String, JMap[String, String]] =
		LazyMaps.create{_: String => JMap[String, String]() }.asJava
}
