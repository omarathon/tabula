package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import scala.collection.JavaConverters.asScalaBufferConverter
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPointSet, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.CurrentUser

object SetMonitoringCheckpointCommand {
	def apply(monitoringPoint: MonitoringPoint, week: Int, user: CurrentUser) =
		new SetMonitoringCheckpointCommand(monitoringPoint, week, user)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointCommandPermissions
			with SetMonitoringPointDescription
			with SetMonitoringCheckpointState
			with AutowiringProfileServiceComponent
			with AutowiringMonitoringPointServiceComponent {
		override lazy val eventName = "SetMonitoringCheckpoint"
	}
}

abstract class SetMonitoringCheckpointCommand(val monitoringPoint: MonitoringPoint, val week: Int, user: CurrentUser)
	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with SelfValidating {
	self: SetMonitoringCheckpointState with ProfileServiceComponent with MonitoringPointServiceComponent =>
	type UniversityId = String

	var studentIds: JList[UniversityId] = JArrayList()
	var members: Seq[StudentMember] = _
	var membersChecked: Seq[StudentMember] = _
	var set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]

	def populate() {
		members = if(set.year == null) {
			profileService.getStudentsByRoute(set.route)
		} else {
			profileService.getStudentsByRoute(
				set.route,
				set.academicYear
			)
		}

		membersChecked = monitoringPointService.getCheckedStudents(monitoringPoint)
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		// convert list of university ids from form into StudentMembers
		val studentMembers = studentIds.asScala.toSeq.map(profileService.getMemberByUniversityId(_).head.asInstanceOf[StudentMember])

		monitoringPointService.updateCheckedStudents(monitoringPoint, studentMembers, user)
	}

	def validate(errors: Errors) {
		if(set.sentToAcademicOffice) {
			errors.reject("monitoringCheckpoint.sentToAcademicOffice.set")
		}

		if(monitoringPoint == null) {
			errors.rejectValue("monitoringPoint", "monitoringPoint")
		}
	}

}

trait SetMonitoringCheckpointCommandPermissions extends RequiresPermissionsChecking {
	self: SetMonitoringCheckpointState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Manage, monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route)
	}
}


trait SetMonitoringPointDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: SetMonitoringCheckpointState =>
	def describe(d: Description) {
		d.monitoringCheckpoint(monitoringPoint)
	}
}


trait SetMonitoringCheckpointState {
	val monitoringPoint : MonitoringPoint
	val week: Int
}