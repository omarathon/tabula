package uk.ac.warwick.tabula.attendance.commands.view.old

import uk.ac.warwick.tabula.data.model.StudentMember
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, AttendanceState}
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{MonitoringPointServiceComponent, TermServiceComponent}
import scala.collection.JavaConverters._

trait GroupedPointValidation {

	self: MonitoringPointServiceComponent with TermServiceComponent =>

	def validateGroupedPoint(
		errors: Errors,
		templateMonitoringPoint: MonitoringPoint,
		studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]]
	) = {
		val academicYear = templateMonitoringPoint.pointSet.academicYear
		val thisAcademicYear = AcademicYear.guessSITSAcademicYearByDate(DateTime.now)
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)

		studentsStateAsScala.foreach{ case(student, pointMap) =>
			val studentPointSet = monitoringPointService.getPointSetForStudent(student, academicYear)
			pointMap.foreach{ case(point, state) =>
				errors.pushNestedPath(s"studentsState[${student.universityId}][${point.id}]")
				val pointSet = point.pointSet
				// Check point is valid for student
				if (!studentPointSet.exists(s => s.points.asScala.contains(point))) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
					// Check has permission for each point
				}	else {

					if (!monitoringPointService.findNonReportedTerms(Seq(student),
						pointSet.academicYear).contains(
							termService.getTermFromAcademicWeekIncludingVacations(point.validFromWeek, pointSet.academicYear).getTermTypeAsString)
					){
						errors.rejectValue("", "monitoringCheckpoint.student.alreadyReportedThisTerm")
					}

					if (thisAcademicYear.startYear <= academicYear.startYear
						&& currentAcademicWeek < point.validFromWeek
						&& !(state == null || state == AttendanceState.MissedAuthorised)
					) {
						if (state == AttendanceState.MissedUnauthorised) errors.rejectValue("", "monitoringCheckpoint.missedUnauthorised.beforeStart")
						else if (state == AttendanceState.Attended) errors.rejectValue("", "monitoringCheckpoint.attended.beforeStart")
					}
				}
				errors.popNestedPath()
			}}
	}

}
