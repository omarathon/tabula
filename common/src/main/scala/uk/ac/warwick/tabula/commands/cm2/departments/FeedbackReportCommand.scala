package uk.ac.warwick.tabula.commands.cm2.departments

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{Errors, ValidationUtils}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.departments.FeedbackReportCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.coursework.FeedbackReportJob
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.jobs.{AutowiringJobServiceComponent, JobInstance, JobServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.validators.WithinYears
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}

object FeedbackReportCommand {
	type Result = JobInstance
	type Command = Appliable[Result] with FeedbackReportCommandState with SelfValidating

	val AdminPermission = Permissions.Department.DownloadFeedbackReport

	def apply(department: Department, user: CurrentUser): Command =
		new FeedbackReportCommandInternal(department, user)
			with FeedbackReportCommandRequest
			with ComposableCommand[Result]
			with FeedbackReportCommandPermissions
			with FeedbackReportCommandValidation
			with FeedbackReportCommandDescription
			with AutowiringJobServiceComponent
}

trait FeedbackReportCommandState {
	def department: Department
	def user: CurrentUser
}

trait FeedbackReportCommandRequest {
	self: FeedbackReportCommandState =>

	@WithinYears(maxFuture = 3, maxPast = 3)
	@DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var startDate: DateTime = DateTime.now.minusMonths(3).withTimeAtStartOfDay()

	@WithinYears(maxFuture = 3, maxPast = 3)
	@DateTimeFormat(pattern = DateFormats.DateTimePickerPattern)
	var endDate: DateTime = DateTime.now.plusDays(1).withTimeAtStartOfDay()
}

class FeedbackReportCommandInternal(val department: Department, val user: CurrentUser)
	extends CommandInternal[Result] with FeedbackReportCommandState {
	self: FeedbackReportCommandRequest
		with JobServiceComponent =>

	override def applyInternal(): Result =
		jobService.add(Option(user), FeedbackReportJob(department, startDate, endDate))

}

trait FeedbackReportCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: FeedbackReportCommandState =>

	override def permissionsCheck(p: PermissionsChecking): Unit = {
		p.PermissionCheck(AdminPermission, mandatory(department))
	}
}

trait FeedbackReportCommandValidation extends SelfValidating {
	self: FeedbackReportCommandRequest =>

	override def validate(errors: Errors): Unit = {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "startDate", "feedback.report.emptyDate")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "endDate", "feedback.report.emptyDate")
		if (endDate.isBefore(startDate)) {
			errors.rejectValue("startDate", "feedback.report.dateRange")
		}
	}
}

trait FeedbackReportCommandDescription extends Describable[Result] {
	self: FeedbackReportCommandState with FeedbackReportCommandRequest =>

	override def describe(d: Description): Unit =
		d.department(department).properties(
			"startDate" -> startDate,
			"endDate" -> endDate
		)

	override def describeResult(d: Description, result: Result): Unit =
		d.property("jobInstance" -> result.id)
}