package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentMember
import org.joda.time.DateTime
import uk.ac.warwick.tabula.services.{TermServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPoint}
import uk.ac.warwick.tabula.commands.TaskBenchmarking

case class StudentPointsData(
	student: StudentMember,
	pointsByTerm: Map[String, Map[MonitoringPoint, String]],
	unrecorded: Int,
	missed: Int
)


trait BuildStudentPointsData extends MonitoringPointServiceComponent with TermServiceComponent with GroupMonitoringPointsByTerm with TaskBenchmarking {

	def buildData(
		students: Seq[StudentMember],
		academicYear: AcademicYear,
		missedCounts: Seq[(StudentMember, Int)] = Seq(),
		unrecordedCounts: Seq[(StudentMember, Int)] = Seq()
	) = {
		val pointSetsByStudent = benchmarkTask("Find point sets for students, by student") { monitoringPointService.findPointSetsForStudentsByStudent(students, academicYear) }
		val allPoints = pointSetsByStudent.flatMap(_._2.points.asScala).toSeq
		val checkpoints = benchmarkTask("Get checkpoints for all students") { monitoringPointService.getCheckpointsByStudent(allPoints) }
		val currentAcademicWeek = benchmarkTask("Get current academic week") { termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear) }

		students.map{ student => {
			pointSetsByStudent.get(student).map{ pointSet =>
				val pointsByTerm = groupByTerm(pointSetsByStudent(student).points.asScala, academicYear)
				val pointsByTermWithCheckpointString = pointsByTerm.map{ case(term, points) =>
					term -> points.map{ point =>
						point -> {
							val checkpointOption = checkpoints.find{
								case (s, checkpoint) => s == student && checkpoint.point == point
							}
							checkpointOption.map{	case (_, checkpoint) => checkpoint.state.dbValue }.getOrElse({
								if (currentAcademicWeek > point.requiredFromWeek)	"late"
								else ""
							})
						}
					}.toMap
				}
				
				val unrecorded = {
					if (unrecordedCounts.size > 0)
						unrecordedCounts.find{case(s, count) => student == s}.getOrElse((student, 0))._2
					else
						pointsByTermWithCheckpointString.values.flatMap(_.values).count(_ == "late")
				}
				val missed = {
					if (missedCounts.size > 0)
						missedCounts.find{case(s, count) => student == s}.getOrElse((student, 0))._2
					else
						pointsByTermWithCheckpointString.values.flatMap(_.values).count(_ == AttendanceState.MissedUnauthorised.dbValue)
				}
				StudentPointsData(student, pointsByTermWithCheckpointString, unrecorded, missed)
			}.getOrElse(
				StudentPointsData(student, Map(), 0, 0)
			)
		}}
	}

}
