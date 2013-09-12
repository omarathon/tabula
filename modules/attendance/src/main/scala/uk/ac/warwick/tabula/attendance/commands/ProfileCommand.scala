package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.StudentCourseDetails
import uk.ac.warwick.tabula.commands.{ReadOnly, Unaudited, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringPoint}
import scala.collection.JavaConverters._
import org.joda.time.DateTime
import uk.ac.warwick.tabula.permissions.Permissions

object ProfileCommand {
	def apply(studentCourseDetails: StudentCourseDetails, academicYear: AcademicYear) =
		new ProfileCommand(studentCourseDetails, academicYear)
		with ComposableCommand[Unit]
		with ProfilePermissions
		with ProfileCommandState
		with AutowiringTermServiceComponent
		with AutowiringRouteServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with ReadOnly with Unaudited
}


abstract class ProfileCommand(val studentCourseDetails: StudentCourseDetails, val academicYear: AcademicYear)
	extends CommandInternal[Unit] with GroupMonitoringPointsByTerm with ProfileCommandState {

	self: RouteServiceComponent with MonitoringPointServiceComponent =>

	override def applyInternal() = {
		studentCourseDetails.studentCourseYearDetails.asScala.find(_.academicYear == academicYear) match {
			case None => throw new ItemNotFoundException
			case Some(studentCourseYearDetail) => {
				routeService.findMonitoringPointSet(
					studentCourseDetails.route,
					studentCourseYearDetail.academicYear,
					Option(studentCourseYearDetail.yearOfStudy)
				) match {
					case None => {
						routeService.findMonitoringPointSet(studentCourseDetails.route, studentCourseYearDetail.academicYear, None) match {
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

		//val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now, pointSet.academicYear)
		val currentAcademicWeek = 50
		checkpointState = monitoringPointService
			.getCheckedForWeek(Seq(studentCourseDetails.student), pointSet, currentAcademicWeek)(studentCourseDetails.student)
			.map{
			case (point, option) => point.id -> option
		}

		missedCountByTerm = monitoringPointsByTerm.map{
			case (termName, points) => termName -> points.count(p => !checkpointState(p.id).getOrElse(true))
		}.filter{
			case (termName, count) => count > 0
		}
	}
}

trait ProfilePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ProfileCommandState =>

	override def permissionsCheck(p: PermissionsChecking) = {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, mandatory(studentCourseDetails.route))
	}

}

trait ProfileCommandState {
	def studentCourseDetails: StudentCourseDetails
	def academicYear: AcademicYear

	var monitoringPointsByTerm: Map[String, Seq[MonitoringPoint]] = _
	var checkpointState: Map[String, Option[Boolean]] = _
	var missedCountByTerm: Map[String, Int] = _
}
