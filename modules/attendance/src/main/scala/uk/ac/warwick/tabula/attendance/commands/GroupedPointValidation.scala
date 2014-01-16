package uk.ac.warwick.tabula.attendance.commands

import uk.ac.warwick.tabula.data.model.{Route, StudentMember}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringPoint, AttendanceState, MonitoringPointSet}
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.services.{SecurityServiceComponent, MonitoringPointServiceComponent, TermServiceComponent}
import scala.collection.JavaConverters._

trait GroupedPointValidation {

	self: MonitoringPointServiceComponent with TermServiceComponent with SecurityServiceComponent =>

	def validateGroupedPoint(
		errors: Errors,
		templateMonitoringPoint: MonitoringPoint,
		studentsStateAsScala: Map[StudentMember, Map[MonitoringPoint, AttendanceState]],
		permissionValidation: (StudentMember, Route) => (Boolean)
	) = {
		val academicYear = templateMonitoringPoint.pointSet.asInstanceOf[MonitoringPointSet].academicYear
		val thisAcademicYear = AcademicYear.guessByDate(DateTime.now)
		val currentAcademicWeek = termService.getAcademicWeekForAcademicYear(DateTime.now(), academicYear)
		studentsStateAsScala.foreach{ case(student, pointMap) =>
			val studentPointSet = monitoringPointService.getPointSetForStudent(student, academicYear)
			pointMap.foreach{ case(point, state) =>
				errors.pushNestedPath(s"studentsState[${student.universityId}][${point.id}]")
				val pointSet = point.pointSet.asInstanceOf[MonitoringPointSet]
				// Check point is valid for student
				if (!studentPointSet.exists(s => s.points.asScala.contains(point))) {
					errors.rejectValue("", "monitoringPoint.invalidStudent")
					// Check has permission for each point
				}	else if (permissionValidation(student, pointSet.route)) {
					errors.rejectValue("", "monitoringPoint.noRecordPermission")
				} else {

					if (!monitoringPointService.findNonReportedTerms(Seq(student),
						pointSet.academicYear).contains(
							termService.getTermFromAcademicWeek(point.validFromWeek, pointSet.academicYear).getTermTypeAsString)
					){
						errors.rejectValue("", "monitoringCheckpoint.student.alreadyReportedThisTerm")
					}

					if (thisAcademicYear.startYear <= academicYear.startYear
						&& currentAcademicWeek < point.validFromWeek
						&& !(state == null || state == AttendanceState.MissedAuthorised)
					) {
						errors.rejectValue("", "monitoringCheckpoint.beforeValidFromWeek")
					}
				}
				errors.popNestedPath()
			}}
	}

}
