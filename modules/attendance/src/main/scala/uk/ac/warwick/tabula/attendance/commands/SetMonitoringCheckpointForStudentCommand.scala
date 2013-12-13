package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringPointSet, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent, MonitoringPointServiceComponent, AutowiringMonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.{ItemNotFoundException, CurrentUser}
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyMaps
import org.joda.time.DateTime

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
			with AutowiringTermServiceComponent
}

abstract class SetMonitoringCheckpointForStudentCommand(
	val monitoringPoint: MonitoringPoint, val student: StudentMember, user: CurrentUser
)	extends CommandInternal[Seq[MonitoringCheckpoint]] with PopulateOnForm {

	self: SetMonitoringCheckpointForStudentState with ProfileServiceComponent with MonitoringPointServiceComponent =>

	def populate() {
		if (!monitoringPointService.getPointSetForStudent(student, set.academicYear).exists(
			s => s.points.asScala.contains(monitoringPoint))
		) {
			throw new ItemNotFoundException()
		}
		val checkpoints = monitoringPointService.getCheckpointsByStudent(Seq(monitoringPoint))
		studentsState = Map(student -> Map(monitoringPoint -> {
			val checkpointOption = checkpoints.find{
				case (s, checkpoint) => s == student && checkpoint.point == monitoringPoint
			}
			checkpointOption.map{case (_, checkpoint) => checkpoint.state}.getOrElse(null)
		}).asJava).asJava
	}

	def applyInternal(): Seq[MonitoringCheckpoint] = {
		if (!monitoringPointService.getPointSetForStudent(student, set.academicYear).exists(
			s => s.points.asScala.contains(monitoringPoint))
		) {
			throw new ItemNotFoundException()
		}
		studentsState.asScala.flatMap{ case (_, pointMap) =>
			pointMap.asScala.flatMap{ case (point, state) =>
				if (state == null) {
					monitoringPointService.deleteCheckpoint(student, point)
					None
				} else {
					Option(monitoringPointService.saveOrUpdateCheckpoint(student, point, state, user))
				}
			}
		}.toSeq
	}
}

trait SetMonitoringCheckpointForStudentCommandValidation extends SelfValidating {
	self: SetMonitoringCheckpointForStudentState with TermServiceComponent with MonitoringPointServiceComponent =>

	def validate(errors: Errors) {

		val academicYear = templateMonitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].academicYear
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)
		studentsState.asScala.foreach{ case(_, pointMap) => {
			val studentPointSet = monitoringPointService.getPointSetForStudent(student, academicYear)
			pointMap.asScala.foreach{ case(point, state) => {
				errors.pushNestedPath(s"studentsState[${student.universityId}][${point.id}]")
				// Check point is valid for student
				if (!studentPointSet.exists(s => s.points.asScala.contains(point))) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
				}	else {
					// Check state change valid
					if (point.sentToAcademicOffice) {
						errors.rejectValue("", "monitoringCheckpoint.sentToAcademicOffice")
					}
					if (currentAcademicWeek < point.validFromWeek && !(state == null || state == AttendanceState.MissedAuthorised)) {
						errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
					}
				}
				errors.popNestedPath()
			}}
		}}
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
		d.property("checkpoints", studentsState.asScala.map{ case (student, pointMap) =>
			student.universityId -> pointMap.asScala.map{ case(point, state) => point -> {
				if (state == null)
					"null"
				else
					state.dbValue
			}}
		})
	}
}


trait SetMonitoringCheckpointForStudentState {
	def monitoringPoint: MonitoringPoint
	def student: StudentMember
	lazy val templateMonitoringPoint = monitoringPoint

	var members: Seq[StudentMember] = _
	var studentsState: JMap[StudentMember, JMap[MonitoringPoint, AttendanceState]] =
		LazyMaps.create{student: StudentMember => JHashMap(): JMap[MonitoringPoint, AttendanceState] }.asJava
	var set = monitoringPoint.pointSet.asInstanceOf[MonitoringPointSet]
}