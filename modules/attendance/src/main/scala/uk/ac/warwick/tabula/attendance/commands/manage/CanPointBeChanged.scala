package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.services.MonitoringPointServiceComponent
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPoint
import uk.ac.warwick.tabula.AcademicYear
import scala.collection.JavaConverters._

trait CanPointBeChanged extends MonitoringPointServiceComponent {

	// TAB-1079
	def canPointBeUpdated(point: MonitoringPoint) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0
	}

	// TAB-1079
	def canPointBeRemoved(point: MonitoringPoint, year: AcademicYear, term: String) = {
		!point.sentToAcademicOffice && monitoringPointService.countCheckpointsForPoint(point) == 0 &&
			!anyStudentsHaveBeenReported(point, year, term)
	}

	// TAB-752 have any students already been submitted for this term
	private def anyStudentsHaveBeenReported(point: MonitoringPoint, year: AcademicYear, period: String): Boolean = {
		val checkpoints = monitoringPointService.getCheckpointsByStudent(point.pointSet.points.asScala)
		val studentsWithCheckpoints = checkpoints.map { case (student , checkpoint) => student}
		monitoringPointService.findReports(studentsWithCheckpoints, year, period).size > 0
	}

}
