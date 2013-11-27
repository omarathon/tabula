package uk.ac.warwick.tabula.attendance.commands.report

import uk.ac.warwick.tabula.data.model.{StudentMember, Department}
import uk.ac.warwick.tabula.{ItemNotFoundException, AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.{SelfValidating, Description, Describable, ComposableCommand, CommandInternal}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringPointReport
import uk.ac.warwick.tabula.services.{AutowiringTermServiceComponent, AutowiringMonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.{Errors, BindingResult}
import org.joda.time.DateTime

object ReportStudentsConfirmCommand {
	def apply(department: Department, currentUser: CurrentUser) =
		new ReportStudentsConfirmCommand(department, currentUser)
		with ComposableCommand[Seq[MonitoringPointReport]]
		with ReportStudentsPermissions
		with ReportStudentsConfirmCommandDescription
		with ReportStudentsConfirmState
		with ReportStudentsConfirmCommandValidation
		with AutowiringProfileServiceComponent
		with AutowiringMonitoringPointServiceComponent
		with AutowiringTermServiceComponent
}


abstract class ReportStudentsConfirmCommand(val department: Department, val currentUser: CurrentUser)
	extends CommandInternal[Seq[MonitoringPointReport]] with BindListener with AvailablePeriods with ReportStudentsConfirmState{

	self: ProfileServiceComponent =>

	def applyInternal() = {
		unreportedStudentsWithMissed.map{case(student, missedCount) =>
			val scd = student.mostSignificantCourseDetails.getOrElse(throw new ItemNotFoundException())
			val report = new MonitoringPointReport
			report.academicYear = academicYear
			report.createdDate = DateTime.now
			report.missed = missedCount
			report.monitoringPeriod = period
			report.reporter = currentUser.apparentId
			report.student = student
			report.studentCourseDetails = scd
			report.studentCourseYearDetails = scd.freshStudentCourseYearDetails.find(_.academicYear == academicYear).getOrElse(throw new ItemNotFoundException())
			monitoringPointService.saveOrUpdate(report)
			report
		}
	}

	def onBind(result: BindingResult) = {
		// Find the students matching the filter
		allStudents = profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = buildOrders()
		)
		// Find the available monitoring periods for those students
		availablePeriods = getAvailablePeriods(allStudents, academicYear)
		// Filter the students to those that are unreported for this period and have missed points
		val studentsWithMissed = monitoringPointService.studentsByMissedCount(
			allStudents.map(_.universityId),
			academicYear,
			isAscending = false,
			Int.MaxValue,
			0
		).filter(_._2 > 0)
		val nonReported = monitoringPointService.findNonReported(studentsWithMissed.map(_._1), academicYear, period)
		unreportedStudentsWithMissed = studentsWithMissed.filter{case(student, count) => nonReported.contains(student)}

		thisPeriod = termService.getTermFromDateIncludingVacations(DateTime.now).getTermTypeAsString
	}
}

trait ReportStudentsConfirmCommandValidation extends SelfValidating {

	self: ReportStudentsConfirmState =>

	def validate(errors: Errors) {
		if (!availablePeriods.filter(_._2).map(_._1).contains(period)) {
			errors.rejectValue("period", "monitoringPointReport.invalidPeriod")
		}
		if (unreportedStudentsWithMissed.isEmpty) {
			errors.rejectValue("unreportedStudentsWithMissed", "monitoringPointReport.noStudents")
		}
		if (!confirm) {
			errors.rejectValue("confirm", "monitoringPointReport.confirm")
		}
	}

}

trait ReportStudentsConfirmCommandDescription extends Describable[Seq[MonitoringPointReport]] {
	self: ReportStudentsConfirmState =>

	override lazy val eventName = "MonitoringPointReport"

	def describe(d: Description) {
		d.property("monitoringPeriod", period)
		d.property("students", unreportedStudentsWithMissed.map{case(student, count) => student.universityId -> count})
	}
}

trait ReportStudentsConfirmState extends ReportStudentsState {
	def department: Department
	def currentUser: CurrentUser

	var academicYear: AcademicYear = _
	var unreportedStudentsWithMissed: Seq[(StudentMember, Int)] = _
	var thisPeriod: String = _
	var thisAcademicYear: AcademicYear = AcademicYear.guessByDate(DateTime.now)

	var confirm: Boolean = false
}
