package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands.{Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointSet
import uk.ac.warwick.tabula.services._
import scala.Some
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions

object MonitoringPointSetReportCommand {
	def apply(monitoringPointSet: MonitoringPointSet) =
		new MonitoringPointSetReportCommand(monitoringPointSet)
		with ComposableCommand[Unit]
		with MonitoringPointSetReportCommandDescription
		with MonitoringPointSetReportPermission
		with AutowiringMonitoringPointServiceComponent
		with AutowiringProfileServiceComponent
		with AutowiringTermServiceComponent
}


abstract class MonitoringPointSetReportCommand(val monitoringPointSet: MonitoringPointSet)
	extends CommandInternal[Unit] with MembersForPointSet with TermServiceComponent with MonitoringPointSetReportCommandState {

	self: MonitoringPointServiceComponent with ProfileServiceComponent =>

	def populate() {
		val members = getMembers(monitoringPointSet)
		//val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
		val currentAcademicWeek = 20
		missedPointCount = monitoringPointService.getCheckedForWeek(members, monitoringPointSet, currentAcademicWeek).map{
			case (member, pointMap) => (member, pointMap.count{
				case (_, Some(false)) => true
				case _ => false
			})
		}.toSeq.sortBy{case (member, count) => (count, member.lastName)}


	}

	def applyInternal() {
		val members = getMembers(monitoringPointSet)
		//val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(new DateTime(), academicYear)
		val currentAcademicWeek = 20

		missedPointCount = monitoringPointService.getCheckedForWeek(members, monitoringPointSet, currentAcademicWeek).map{
			case (member, pointMap) => (member, pointMap.count{
				case (_, Some(true)) => true
				case _ => false
			})
		}.toSeq

		// TODO: Push counts to SITS

		monitoringPointService.setSentToAcademicOfficeForWeek(monitoringPointSet, currentAcademicWeek)
	}

}

trait MonitoringPointSetReportPermission extends RequiresPermissionsChecking {
	self: MonitoringPointSetReportCommandState =>
	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, monitoringPointSet.route)
	}
}

trait MonitoringPointSetReportCommandDescription extends Describable[Unit] {
	self: MonitoringPointSetReportCommandState =>

	override lazy val eventName = "MonitoringPointSetReport"

	override def describe(d: Description) {
		d.monitoringPointSet(monitoringPointSet)
		d.property("missedPoints", missedPointCount.map{case (member, count) => (member.universityId, count)})
	}
}

trait MonitoringPointSetReportCommandState {

	def monitoringPointSet: MonitoringPointSet

	var missedPointCount: Seq[(StudentMember, Int)] = _
}
