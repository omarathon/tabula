package uk.ac.warwick.tabula.attendance.commands.view

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.data.model.{Department, AttendanceNote, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpointTotal, AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint}
import uk.ac.warwick.tabula.services.TermServiceComponent
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent

case class FilteredStudentResult(
	student: StudentMember,
	groupedPointCheckpointPairs: Map[String, Seq[(AttendanceMonitoringPoint, AttendanceMonitoringCheckpoint)]],
	attendanceNotes: Map[AttendanceMonitoringPoint, AttendanceNote],
	checkpointTotal: AttendanceMonitoringCheckpointTotal
)

case class FilteredStudentsAttendanceResult(
	totalResults: Int,
	results: Seq[FilteredStudentResult],
	students: Seq[StudentMember]
)

trait BuildsFilteredStudentsAttendanceResult extends TaskBenchmarking with GroupsPoints {
	
	self: AttendanceMonitoringServiceComponent with TermServiceComponent =>
	
	def buildAttendanceResult(
		totalResults: Int,
		students: Seq[StudentMember],
		departmentOption: Option[Department],
		academicYear: AcademicYear,
		pointMap: Map[StudentMember, Seq[AttendanceMonitoringPoint]] = Map()
	): FilteredStudentsAttendanceResult = {
		val results = benchmarkTask("Build FilteredStudentResults"){ students.map { student =>
			val points = pointMap.getOrElse(student, benchmarkTask("listStudentsPoints") {
				attendanceMonitoringService.listStudentsPoints(student, departmentOption, academicYear)
			})
			val checkpointMap = benchmarkTask("getCheckpoints") {
				attendanceMonitoringService.getCheckpoints(points, student)
			}
			val groupedPoints = benchmarkTask("groupedPoints") {
				groupByTerm(points, groupSimilar = false) ++ groupByMonth(points, groupSimilar = false)
			}
			val groupedPointCheckpointPairs = benchmarkTask("groupedPointCheckpointPairs") {
				groupedPoints.map { case (period, thesePoints) =>
					period -> thesePoints.map { groupedPoint =>
						groupedPoint.templatePoint -> checkpointMap.getOrElse(groupedPoint.templatePoint, null)
					}
				}
			}
			val attendanceNotes = benchmarkTask("attendanceNotes") {
				attendanceMonitoringService.getAttendanceNoteMap(student)
			}
			val checkpointTotal = benchmarkTask("checkpointTotal") {
				attendanceMonitoringService.getCheckpointTotal(student, departmentOption, academicYear)
			}
			FilteredStudentResult(student, groupedPointCheckpointPairs, attendanceNotes, checkpointTotal)
		}}
		FilteredStudentsAttendanceResult(totalResults, results, students)
	}
}