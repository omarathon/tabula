package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.services.{TermServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._

trait CanPointBeChanged extends MonitoringPointServiceComponent with TermServiceComponent {

	// TAB-1079
	def canPointBeUpdated(point: MonitoringPoint) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0 &&
			!anyStudentsReportedForRelatedPointsThisTerm(point)
	}

	// TAB-1079
	def canPointBeRemoved(point: MonitoringPoint) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0 &&
			!anyStudentsReportedForRelatedPointsThisTerm(point)
	}

	// TAB-752 have any students already been submitted for this term
	def anyStudentsReportedForRelatedPointsThisTerm (point: MonitoringPoint): Boolean = {
		val checkpoints = monitoringPointService.getCheckpointsByStudent(point.pointSet.points.asScala)
		if (checkpoints.isEmpty) return false
		val studentsWithCheckpoints = checkpoints.map { case (student , checkpoint) => student}
		monitoringPointService.findReports(studentsWithCheckpoints, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear,
			termService.getTermFromAcademicWeek(point.validFromWeek, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear).getTermTypeAsString).size > 0
	}

}
