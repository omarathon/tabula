package uk.ac.warwick.tabula.commands.reports

import org.joda.time.LocalDate
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.SelfValidating
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

trait ReportPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: ReportCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.Reports, department)
	}

}

trait ReportCommandState {
	def department: Department
	def academicYear: AcademicYear
}

trait ReportCommandRequest {
	self: ReportCommandState =>

	var startDate: LocalDate = {
		val twoWeeksAgo = LocalDate.now.minusWeeks(2)
		if (twoWeeksAgo.isBefore(academicYear.firstDay))
			academicYear.firstDay
		else if (twoWeeksAgo.isAfter(academicYear.lastDay))
			academicYear.lastDay.minusWeeks(4)
		else
			twoWeeksAgo
	}

	var endDate: LocalDate = {
		val twoWeeksTime = startDate.plusWeeks(4)
		if (twoWeeksTime.isAfter(academicYear.lastDay))
			academicYear.lastDay
		else
			twoWeeksTime
	}
}

trait ReportCommandRequestValidation extends SelfValidating {
	self: ReportCommandRequest with ReportCommandState =>

	override def validate(errors: Errors): Unit = {
		if (startDate == null) errors.rejectValue("startDate", "NotEmpty")
		if (endDate == null) errors.rejectValue("endDate", "NotEmpty")

		if (startDate != null && endDate != null) {
			if (endDate.isBefore(startDate)) errors.rejectValue("endDate", "reports.dates.endBeforeStart")

			if (startDate.isBefore(academicYear.firstDay)) errors.rejectValue("startDate", "reports.dates.min")
			else if (startDate.isAfter(academicYear.lastDay)) errors.rejectValue("startDate", "reports.dates.max")

			if (endDate.isBefore(academicYear.firstDay)) errors.rejectValue("endDate", "reports.dates.min")
			else if (endDate.isAfter(academicYear.lastDay)) errors.rejectValue("endDate", "reports.dates.max")
		}
	}
}