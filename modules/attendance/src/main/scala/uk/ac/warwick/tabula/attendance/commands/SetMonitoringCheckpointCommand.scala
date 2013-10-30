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

object SetMonitoringCheckpointCommand {
	def apply(monitoringPoint: MonitoringPoint, user: CurrentUser) =
		new SetMonitoringCheckpointCommand(monitoringPoint, user)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointCommandPermissions
			with SetMonitoringCheckpointCommandValidation
			with SetMonitoringPointDescription
			with SetMonitoringCheckpointState
			with AutowiringProfileServiceComponent
			with AutowiringMonitoringPointServiceComponent
}

abstract class SetMonitoringCheckpointCommand(val monitoringPoint: MonitoringPoint, user: CurrentUser)
	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with MembersForPointSet {

	self: SetMonitoringCheckpointState with ProfileServiceComponent with MonitoringPointServiceComponent =>

	def populate() {
		set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
		members = getMembers(set)
		studentsState = monitoringPointService.getCheckpointsBySCD(monitoringPoint).map{
			case (studentCourseDetails, checkpoint) => studentCourseDetails.student.universityId -> checkpoint.state
		}.toMap.asJava
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
		members = getMembers(set)
		studentsState.asScala.map{ case (universityId, state) =>
			val route = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
			val scjCode = members.find(member => member.universityId == universityId) match {
				case None => throw new ItemNotFoundException()
				case Some(member) => member.studentCourseDetails.asScala.find(scd => scd.route == route) match {
					case None => throw new ItemNotFoundException()
					case Some(scd) => scd.scjCode
				}
			}
			if (state == null) {
				monitoringPointService.deleteCheckpoint(scjCode, monitoringPoint)
				None
			} else {
				profileService.getStudentCourseDetailsByScjCode(scjCode) match {
					case None => throw new ItemNotFoundException()
					case Some(scd) => Option(monitoringPointService.saveOrUpdateCheckpoint(scd, monitoringPoint, state, user))
				}
			}
		}.flatten.toSeq
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

trait SetMonitoringCheckpointCommandPermissions extends RequiresPermissionsChecking with PermissionsChecking {
	self: SetMonitoringCheckpointState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, mandatory(monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]))
	}
}


trait SetMonitoringPointDescription extends Describable[Seq[MonitoringCheckpoint]] {
	self: SetMonitoringCheckpointState =>

	override lazy val eventName = "SetMonitoringCheckpoint"

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


trait SetMonitoringCheckpointState {
	def monitoringPoint: MonitoringPoint

	type UniversityId = String

	var members: Seq[StudentMember] = _
	var studentsState: JMap[UniversityId, MonitoringCheckpointState] = JHashMap()
	var set : MonitoringPointSet = _
}