package uk.ac.warwick.tabula.attendance.commands

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringCheckpoint, AttendanceMonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object StudentRecordCommand {
	def apply(academicYear: AcademicYear, student: StudentMember, user: CurrentUser) =
		new StudentRecordCommandInternal(academicYear, student, user)
			with ComposableCommand[Seq[AttendanceMonitoringCheckpoint]]
			with PopulatesAgentStudentRecordCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with StudentRecordValidation
			with StudentRecordDescription
			with StudentRecordPermissions
			with StudentRecordCommandState
}


class StudentRecordCommandInternal(val academicYear: AcademicYear, val student: StudentMember, val user: CurrentUser)
	extends CommandInternal[Seq[AttendanceMonitoringCheckpoint]] {

	self: StudentRecordCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.setAttendance(student, checkpointMap.asScala.toMap, user)
	}

}

trait PopulatesAgentStudentRecordCommand extends PopulateOnForm {

	self: StudentRecordCommandState with AttendanceMonitoringServiceComponent =>

	override def populate() = {
		val points = attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		val checkpoints = attendanceMonitoringService.getCheckpoints(points, student)
		points.foreach(p => checkpointMap.put(p, checkpoints.get(p).map(_.state).orNull))
	}
}

trait StudentRecordValidation extends SelfValidating {

	self: StudentRecordCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def validate(errors: Errors) = {
		val points = attendanceMonitoringService.listStudentsPoints(student, None, academicYear)
		val nonReportedTerms = attendanceMonitoringService.findNonReportedTerms(Seq(student), academicYear)

		checkpointMap.asScala.foreach { case (point, state) =>
			errors.pushNestedPath(s"checkpointMap[${point.id}]")

			if (!points.contains(point)) {
				// should never be the case, but protect against POST-hack
				errors.rejectValue("","Submitted attendance for a point which is student is not attending")
			}

			if (!nonReportedTerms.contains(termService.getTermFromDateIncludingVacations(point.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)) {
				errors.rejectValue("", "attendanceMonitoringCheckpoint.alreadyReportedThisTerm")
			}

			if (point.isStartDateInFuture && !(state == null || state == AttendanceState.MissedAuthorised)) {
				if (state == AttendanceState.MissedUnauthorised) errors.rejectValue("", "monitoringCheckpoint.missedUnauthorised.beforeStart")
				else if (state == AttendanceState.Attended) errors.rejectValue("", "monitoringCheckpoint.attended.beforeStart")
			}

			errors.popNestedPath()
		}
	}

}

trait StudentRecordPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: StudentRecordCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}

}

trait StudentRecordDescription extends Describable[Seq[AttendanceMonitoringCheckpoint]] {

	self: StudentRecordCommandState =>

	override lazy val eventName = "StudentRecord"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
	}
}

trait StudentRecordCommandState {
	def academicYear: AcademicYear
	def student: StudentMember
	def user: CurrentUser

	// Bind variables

	var checkpointMap: JMap[AttendanceMonitoringPoint, AttendanceState] = JHashMap()
}
