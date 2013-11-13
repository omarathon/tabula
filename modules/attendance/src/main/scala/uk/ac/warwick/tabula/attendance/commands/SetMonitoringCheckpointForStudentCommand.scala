package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

object SetMonitoringCheckpointForStudentCommand {
	def apply(monitoringPoint: MonitoringPoint, student: StudentMember, user: CurrentUser) =
		new SetMonitoringCheckpointForStudentCommand(monitoringPoint, student, user)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointForStudentCommandPermissions
			with SetMonitoringCheckpointForStudentCommandValidation
			with SetMonitoringPointForStudentDescription
			with SetMonitoringCheckpointForStudentState
			with AutowiringProfileServiceComponent
			with AutowiringMonitoringPointServiceComponent
}

abstract class SetMonitoringCheckpointForStudentCommand(
	val monitoringPoint: MonitoringPoint, val student: StudentMember, user: CurrentUser
)	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] {

	self: SetMonitoringCheckpointForStudentState with ProfileServiceComponent with MonitoringPointServiceComponent =>

	def populate() {
		if (!monitoringPointService.getPointSetForStudent(student, set.academicYear).exists(
			s => s.points.asScala.contains(monitoringPoint))
		) {
			throw new ItemNotFoundException()
		}
		studentsState = monitoringPointService.getCheckpointsByStudent(Seq(monitoringPoint)).map{
			case (s, checkpoint) => s -> checkpoint.state
		}.toMap.asJava
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		if (!monitoringPointService.getPointSetForStudent(student, set.academicYear).exists(
			s => s.points.asScala.contains(monitoringPoint))
		) {
			throw new ItemNotFoundException()
		}
		studentsState.asScala.map{ case (s, state) =>
			if (state == null) {
				monitoringPointService.deleteCheckpoint(student, monitoringPoint)
				None
			} else {
				Option(monitoringPointService.saveOrUpdateCheckpoint(student, monitoringPoint, state, user))
			}
		}.flatten.toSeq
	}
}

trait SetMonitoringCheckpointForStudentCommandValidation extends SelfValidating {
	self: SetMonitoringCheckpointForStudentState =>

	def validate(errors: Errors) {

		if (monitoringPoint.sentToAcademicOffice) {
			errors.reject("monitoringCheckpoint.sentToAcademicOffice")
		}

		if (monitoringPoint == null) {
			errors.rejectValue("monitoringPoint", "monitoringPoint")
		}
	}

}

trait SetMonitoringCheckpointForStudentCommandPermissions extends RequiresPermissionsChecking {
	self: SetMonitoringCheckpointForStudentState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}
}


trait SetMonitoringPointForStudentDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: SetMonitoringCheckpointForStudentState =>

	override lazy val eventName = "SetMonitoringCheckpointForStudent"

	def describe(d: Description) {
		d.monitoringCheckpoint(monitoringPoint)
		d.property("checkpoints", studentsState.asScala.map{ case (universityId, state) =>
			if (state == null)
				universityId -> "null"
			else
				universityId -> state.dbValue
		})
	}
}


trait SetMonitoringCheckpointForStudentState {
	def monitoringPoint: MonitoringPoint
	def student: StudentMember

	var members: Seq[StudentMember] = _
	var studentsState: JMap[StudentMember, MonitoringCheckpointState] = JHashMap()
	var set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
}