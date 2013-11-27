package uk.ac.warwick.tabula.attendance.commands.report

import uk.ac.warwick.tabula.commands.{SelfValidating, FiltersStudents, ReadOnly, Unaudited, CommandInternal, ComposableCommand}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import org.hibernate.criterion.Order._
import uk.ac.warwick.tabula.JavaImports._
import org.hibernate.criterion.Order
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, AutowiringTermServiceComponent, MonitoringPointServiceComponent, ProfileServiceComponent, AutowiringProfileServiceComponent}
import uk.ac.warwick.tabula.attendance.commands.GroupMonitoringPointsByTerm
import uk.ac.warwick.tabula.system.BindListener
import org.springframework.validation.{Errors, BindingResult}

object ReportStudentsChoosePeriodCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new ReportStudentsChoosePeriodCommand(department, academicYear)
			with ComposableCommand[Seq[(StudentMember, Int)]]
			with ReportStudentsPermissions
			with ReportStudentsState
			with ReportStudentsChoosePeriodCommandValidation
			with AutowiringProfileServiceComponent
			with AutowiringTermServiceComponent
			with AutowiringMonitoringPointServiceComponent
			with ReadOnly with Unaudited
}

abstract class ReportStudentsChoosePeriodCommand(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[(StudentMember, Int)]] with ReportStudentsState with GroupMonitoringPointsByTerm with BindListener with AvailablePeriods {

	self: ProfileServiceComponent with MonitoringPointServiceComponent =>

	def applyInternal() = {
		val studentsWithMissed = monitoringPointService.studentsByMissedCount(
			allStudents.map(_.universityId),
			academicYear,
			isAscending = false,
			Int.MaxValue,
			0
		).filter(_._2 > 0)
		val nonReported = monitoringPointService.findNonReported(studentsWithMissed.map(_._1), academicYear, period)
		studentsWithMissed.filter{case(student, count) => nonReported.contains(student)}
	}

	def onBind(result: BindingResult) = {
		allStudents = profileService.findAllStudentsByRestrictions(
			department = department,
			restrictions = buildRestrictions(),
			orders = buildOrders()
		)
		availablePeriods = getAvailablePeriods(allStudents, academicYear)
	}
}

trait ReportStudentsChoosePeriodCommandValidation extends SelfValidating {

	self: ReportStudentsState =>

	def validate(errors: Errors) {
		if (!availablePeriods.filter(_._2).map(_._1).contains(period)) {
			errors.rejectValue("period", "monitoringPointReport.invalidPeriod")
		}
	}

}

trait ReportStudentsPermissions extends RequiresPermissionsChecking {
	this: ReportStudentsState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.MonitoringPoints.Report, department)
	}
}

trait ReportStudentsState extends FiltersStudents {
	def department: Department
	def academicYear: AcademicYear

	val defaultOrder = Seq(asc("lastName"), asc("firstName")) // Don't allow this to be changed atm
	var sortOrder: JList[Order] = JArrayList()

	var courseTypes: JList[CourseType] = JArrayList()
	var routes: JList[Route] = JArrayList()
	var modesOfAttendance: JList[ModeOfAttendance] = JArrayList()
	var yearsOfStudy: JList[JInteger] = JArrayList()
	var sprStatuses: JList[SitsStatus] = JArrayList()
	var modules: JList[Module] = JArrayList()

	var period: String = _

	var availablePeriods: Seq[(String, Boolean)] = _
	var allStudents: Seq[StudentMember] = _
}
