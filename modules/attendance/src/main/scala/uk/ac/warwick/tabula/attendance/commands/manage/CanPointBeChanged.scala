package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.services.{TermServiceComponent, MonitoringPointServiceComponent}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.AcademicYear

trait CanPointBeChanged extends MonitoringPointServiceComponent with TermServiceComponent {

	// TAB-1537
	def canPointBeUpdated(point: MonitoringPoint) = {
		!anyStudentsReportedForRelatedPointsThisTerm(point)
	}

	// TAB-1079
	def canPointBeRemoved(point: MonitoringPoint) = {
		monitoringPointService.countCheckpointsForPoint(point) == 0 && !anyStudentsReportedForRelatedPointsThisTerm(point)
	}

	def canPointBeAdded(point: MonitoringPoint) = !anyStudentsReportedForRelatedPointsThisTerm(point)

	// TAB-752 have any students already been submitted for this term
	def anyStudentsReportedForRelatedPointsThisTerm (point: MonitoringPoint): Boolean = {
		anyStudentsReportedForThisTerm(point.pointSet.asInstanceOf[MonitoringPointSet], point.validFromWeek, point.pointSet.asInstanceOf[MonitoringPointSet].academicYear)
	}

	def anyStudentsReportedForThisTerm (set: MonitoringPointSet, validFromWeek: Int, academicYear: AcademicYear): Boolean = {
		val checkpoints = monitoringPointService.getCheckpointsByStudent(set.points.asScala)
		if (checkpoints.isEmpty) false
		else {
			val studentsWithCheckpoints = checkpoints.map { case (student , checkpoint) => student}
			monitoringPointService.findReports(studentsWithCheckpoints, academicYear,
				termService.getTermFromAcademicWeek(validFromWeek, academicYear).getTermTypeAsString).size > 0
		}

	}

}
