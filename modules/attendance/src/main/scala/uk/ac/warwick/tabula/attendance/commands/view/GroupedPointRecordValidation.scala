package uk.ac.warwick.tabula.attendance.commands.view

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceState}
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.attendancemonitoring.AttendanceMonitoringServiceComponent
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, TermServiceComponent}
import uk.ac.warwick.tabula.commands.MemberOrUser

trait GroupedPointRecordValidation {

	self: AttendanceMonitoringServiceComponent with TermServiceComponent with SecurityServiceComponent =>

	def validateGroupedPoint(
		errors: Errors,
		templatePoint: AttendanceMonitoringPoint,
		checkpointMap: Map[StudentMember, Map[AttendanceMonitoringPoint, AttendanceState]],
		user: CurrentUser
	) = {
		checkpointMap.foreach{ case(student, pointMap) =>
			pointMap.foreach{ case(point, state) =>
				errors.pushNestedPath(s"checkpointMap[${student.universityId}][${point.id}]")
				// Check point is valid for student
				if (point.scheme.department != templatePoint.scheme.department && !securityService.can(user, Permissions.MonitoringPoints.Record, student)
					|| !point.scheme.members.includesUser(MemberOrUser(student).asUser)
				) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
				} else {
					// Check not reported
					if (!attendanceMonitoringService.findNonReportedTerms(Seq(student), point.scheme.academicYear).contains(
						termService.getTermFromDateIncludingVacations(templatePoint.startDate.toDateTimeAtStartOfDay).getTermTypeAsString)
					){
						errors.rejectValue("", "monitoringCheckpoint.student.alreadyReportedThisTerm")
					}

					// Check valid state
					if (DateTime.now.isBefore(point.startDate.toDateTimeAtStartOfDay) && !(state == null || state == AttendanceState.MissedAuthorised)) {
						errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
					}
				}
				errors.popNestedPath()
			}}
		}
}
