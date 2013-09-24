package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.permissions.Permissions

object ProfileCommand {
	def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
		new ProfileCommand(studentCourseDetails, academicYear)
		with ComposableCommand[Unit]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly with Unaudited
}


abstract class ProfileCommand(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
	extends CommandInternal[Unit] with GroupMonitoringPointsByTerm with ProfileCommandState {

	self: MonitoringPointServiceComponent =>

	override def applyInternal() = {
		studentCourseDetails.studentCourseYearDetails.asScala.find(_.academicYear == academicYear) match {
			case None => throw new ItemNotFoundException
			case Some(studentCourseYearDetail) => {
				monitoringPointService.findMonitoringPointSet(
					studentCourseDetails.route,
					studentCourseYearDetail.academicYear,
					Option(studentCourseYearDetail.yearOfStudy)
				) match {
					case None => {
						monitoringPointService.findMonitoringPointSet(studentCourseDetails.route, studentCourseYearDetail.academicYear, None) match {
							case None => throw new ItemNotFoundException
							case Some(pointSet) => applyForPointSet(pointSet)
						}
					}
					case Some(pointSet) => applyForPointSet(pointSet)
				}
			}
		}
	}

	private def applyForPointSet(pointSet: MonitoringPointSet) = {
		monitoringPointsByTerm = groupByTerm(pointSet.points.asScala, pointSet.academicYear)
		checkpointState = monitoringPointService
			.getChecked(Seq(studentCourseDetails.student), pointSet)(studentCourseDetails.student)
			.map{	case (point, option) => point.id -> (option match {
				case Some(state) => state.dbValue
				case _ => "late"
			})
		}

		missedCountByTerm = monitoringPointsByTerm.map{
			case (termName, points) => termName -> points.count(
				p => checkpointState(p.id).equals(MonitoringCheckpointState.MissedUnauthorised.dbValue)
			)
		}.filter{
			case (termName, count) => count > 0
		}
	}
}

trait ProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		p.PermissionCheck(Permissions.MonitoringPoints.View, mandatory(studentCourseDetails))
	}

}

trait ProfileCommandState {
	def studentCourseDetails: StudentCourseDetails
	def academicYear: AcademicYear

	var monitoringPointsByTerm: Map[String, Seq[MonitoringPoint]] = _
	var checkpointState: Map[String, String] = _
	var missedCountByTerm: Map[String, Int] = _
}
