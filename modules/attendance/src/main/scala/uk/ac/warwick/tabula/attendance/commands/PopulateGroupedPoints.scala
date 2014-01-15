package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, MonitoringPointSet}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.services.{ProfileServiceComponent, TermServiceComponent, MonitoringPointServiceComponent}
import scala.collection.JavaConverters._

trait PopulateGroupedPoints extends CheckpointUpdatedDescription {

	self: MonitoringPointServiceComponent with TermServiceComponent with ProfileServiceComponent =>

	def populateGroupedPoints(students: Seq[StudentMember], templateMonitoringPoint: MonitoringPoint) = {
		// Get monitoring points by student for the list of students matching the template point
		val studentPointMap = monitoringPointService.findSimilarPointsForMembers(templateMonitoringPoint, students)
		val allPoints = studentPointMap.values.flatten.toSeq
		val pointSet = templateMonitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
		val period = termService.getTermFromAcademicWeek(templateMonitoringPoint.validFromWeek, pointSet.academicYear).getTermTypeAsString
		val nonReported = monitoringPointService.findNonReported(students, pointSet.academicYear, period)
		val checkpoints = monitoringPointService.getCheckpointsByStudent(allPoints)
		// Map the checkpoint state to each point for each student, and filter out any students already reported for this term
		val studentsState = studentPointMap.map{ case (student, points) =>
			student -> points.map{ point =>
				point -> {
					val checkpointOption = checkpoints.find{
						case (s, checkpoint) => s == student && checkpoint.point == point
					}
					checkpointOption.map{case (_, checkpoint) => checkpoint.state}.getOrElse(null)
				}
			}.toMap.asJava
		}.filter{case(student, map) => nonReported.contains(student)}.toMap.asJava

		val checkpointDescriptions = studentsState.asScala.map{
			case (student, pointMap) => student -> pointMap.asScala.map{
				case(point, state) => point -> {
					checkpoints.find{
						case (s, checkpoint) => s == student && checkpoint.point == point
					}.map{case (_, checkpoint) => describeCheckpoint(checkpoint)}.getOrElse("")
				}
			}.toMap}.toMap

		(studentsState, checkpointDescriptions)
	}

}
