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
	def apply(monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new SetMonitoringCheckpointCommand(monitoringPoint, user)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointCommandPermissions
			with SetMonitoringCheckpointCommandValidation
			with SetMonitoringPointDescription
			with SetMonitoringCheckpointState
			with AutowiringProfileServiceComponent
			with AutowiringMonitoringPointServiceComponent {
		override lazy val eventName = "SetMonitoringCheckpoint"
	}
}

abstract class SetMonitoringCheckpointCommand(val monitoringPoint: MonitoringPoint, user: CurrentUser)
	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with MembersForPointSet {

	self: SetMonitoringCheckpointState with ProfileServiceComponent with MonitoringPointServiceComponent =>
	type UniversityId = String

	var studentIds: JList[UniversityId] = JArrayList()
	var members: Seq[StudentMember] = _
	var studentsChecked: Map[String, Boolean] = _
	var set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]

	def populate() {
		members = getMembers(monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet])
		studentsChecked = monitoringPointService.getStudents(monitoringPoint).map {
			case(student, isChecked) => (student.id, isChecked)
		}.toMap
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		// convert list of university ids from form into StudentMembers
		val checkedStudentMembers = studentIds.asScala.toSeq.map(profileService.getMemberByUniversityId(_).head.asInstanceOf[StudentMember])

		members = getMembers(monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet])
		val uncheckedStudentMembers = members.toSet -- checkedStudentMembers.toSet
		val changedStudentMembers = checkedStudentMembers.map(student => (student, true)) ++ uncheckedStudentMembers.map(student => (student, false))
		monitoringPointService.updateStudents(monitoringPoint, changedStudentMembers, user)
	}

}

trait SetMonitoringCheckpointCommandValidation extends SelfValidating {
	self: SetMonitoringCheckpointState =>

	def validate(errors: Errors) {
		if(monitoringPoint.sentToAcademicOffice) {
			errors.reject("monitoringCheckpoint.sentToAcademicOffice")
		}

		if(monitoringPoint == null) {
			errors.rejectValue("monitoringPoint", "monitoringPoint")
		}
	}

}

trait SetMonitoringCheckpointCommandPermissions extends RequiresPermissionsChecking {
	self: SetMonitoringCheckpointState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route)
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
}