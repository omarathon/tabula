package uk.ac.warwick.tabula.commands.attendance.view

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.services.attendancemonitoring.{AutowiringAttendanceMonitoringServiceComponent, AttendanceMonitoringServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState, AttendanceMonitoringCheckpoint}
import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, TermServiceComponent}
import collection.JavaConverters._
import org.joda.time.DateTime

object RecordStudentAttendanceCommand {
	def apply(department: Department, academicYear: AcademicYear, student: StudentMember, user: CurrentUser) =
		new RecordStudentAttendanceCommandInternal(department, academicYear, student, user)
			with ComposableCommand[Seq[AttendanceMonitoringCheckpoint]]
			with PopulatesRecordStudentAttendanceCommand
			with AutowiringAttendanceMonitoringServiceComponent
			with AutowiringTermServiceComponent
			with RecordStudentAttendanceValidation
			with RecordStudentAttendanceDescription
			with RecordStudentAttendancePermissions
			with RecordStudentAttendanceCommandState
}


class RecordStudentAttendanceCommandInternal(val department: Department, val academicYear: AcademicYear, val student: StudentMember, val user: CurrentUser)
	extends CommandInternal[Seq[AttendanceMonitoringCheckpoint]] {

	self: RecordStudentAttendanceCommandState with AttendanceMonitoringServiceComponent =>

	override def applyInternal() = {
		attendanceMonitoringService.setAttendance(student, checkpointMap.asScala.toMap, user)
	}

}

trait PopulatesRecordStudentAttendanceCommand extends PopulateOnForm {

	self: RecordStudentAttendanceCommandState with AttendanceMonitoringServiceComponent =>

	override def populate() = {
		val points = attendanceMonitoringService.listStudentsPoints(student, Option(department), academicYear)
		val checkpoints = attendanceMonitoringService.getCheckpoints(points, student)
		points.foreach(p => checkpointMap.put(p, checkpoints.get(p).map(_.state).orNull))
	}
}

trait RecordStudentAttendanceValidation extends SelfValidating {

	self: RecordStudentAttendanceCommandState with AttendanceMonitoringServiceComponent with TermServiceComponent =>

	override def validate(errors: Errors) = {
		val points = attendanceMonitoringService.listStudentsPoints(student, Option(department), academicYear)
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

			// check that a note exists for authorised absences
			if (state == AttendanceState.MissedAuthorised && attendanceMonitoringService.getAttendanceNote(student, point).isEmpty) {
				errors.rejectValue("", "monitoringCheckpoint.missedAuthorised.noNote")
			}

			errors.popNestedPath()
		}
	}

}

trait RecordStudentAttendancePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: RecordStudentAttendanceCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Record, student)
	}

}

trait RecordStudentAttendanceDescription extends Describable[Seq[AttendanceMonitoringCheckpoint]] {

	self: RecordStudentAttendanceCommandState =>

	override lazy val eventName = "RecordStudentAttendance"

	override def describe(d: Description) {
		d.studentIds(Seq(student.universityId))
	}
}

trait RecordStudentAttendanceCommandState {
	def department: Department
	def academicYear: AcademicYear
	def student: StudentMember
	def user: CurrentUser

	// Bind variables

	var checkpointMap: JMap[AttendanceMonitoringPoint, AttendanceState] = JHashMap()
}
