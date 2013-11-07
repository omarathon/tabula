package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpointState, MonitoringPointSet, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._

object SetMonitoringCheckpointForStudentCommand {
	def apply(monitoringPoint: MonitoringPoint, studentCourseDetails: StudentCourseDetails, user: CurrentUser) =
		new SetMonitoringCheckpointForStudentCommand(monitoringPoint, studentCourseDetails, user)
			with ComposableCommand[Seq[MonitoringCheckpoint]]
			with SetMonitoringCheckpointForStudentCommandPermissions
			with SetMonitoringCheckpointForStudentCommandValidation
			with SetMonitoringPointForStudentDescription
			with SetMonitoringCheckpointForStudentState
			with AutowiringProfileServiceComponent
			with AutowiringMonitoringPointServiceComponent
}

abstract class SetMonitoringCheckpointForStudentCommand(
	val monitoringPoint: MonitoringPoint, val studentCourseDetails: StudentCourseDetails, user: CurrentUser
)	extends CommandInternal[Seq[MonitoringCheckpoint]] with Appliable[Seq[MonitoringCheckpoint]] with MembersForPointSet {

	self: SetMonitoringCheckpointForStudentState with ProfileServiceComponent with MonitoringPointServiceComponent =>

	def populate() {
		val universityId: UniversityId = studentCourseDetails.student.universityId
		members = getMembers(set).filter(m => m.universityId == universityId)
		if (members.size == 0) {
			throw new ItemNotFoundException()
		}
		studentsState = monitoringPointService.getCheckpointsBySCD(Seq(monitoringPoint)).map{
			case (scd, checkpoint) => scd.student.universityId -> checkpoint.state
		}.toMap.filter{case(uniId, _) => uniId == universityId}.asJava
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		val universityId: UniversityId = studentCourseDetails.student.universityId
		members = getMembers(set).filter(m => m.universityId == universityId)
		if (members.size == 0) {
			throw new ItemNotFoundException()
		}
		studentsState.asScala.map{ case (uniId, state) =>
			val route = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].route
			val scjCode = members.find(member => member.universityId == uniId) match {
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

trait SetMonitoringCheckpointForStudentCommandValidation extends SelfValidating {
	self: SetMonitoringCheckpointForStudentState =>

	def validate(errors: Errors) {
		if(monitoringPoint.sentToAcademicOffice) {
			errors.reject("monitoringCheckpoint.sentToAcademicOffice")
		}

		if(monitoringPoint == null) {
			errors.rejectValue("monitoringPoint", "monitoringPoint")
		}
	}

}

trait SetMonitoringCheckpointForStudentCommandPermissions extends RequiresPermissionsChecking {
	self: SetMonitoringCheckpointForStudentState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, studentCourseDetails)
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
	def studentCourseDetails: StudentCourseDetails

	type UniversityId = String

	var members: Seq[StudentMember] = _
	var studentsState: JMap[UniversityId, MonitoringCheckpointState] = JHashMap()
	var set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
}