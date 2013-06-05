package uk.ac.warwick.tabula.coursework.commands.departments

import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.validation.{ValidationUtils, Errors}

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.coursework.jobs.FeedbackReportJob

class FeedbackReportCommand (val department:Department, val user: CurrentUser) extends Command[JobInstance] with ReadOnly
			with Unaudited with SelfValidating {
	
	PermissionCheck(Permissions.Department.DownloadFeedbackReport, department)

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var startDate:DateTime = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	var endDate:DateTime = _

	var jobService = Wire.auto[JobService]

	def applyInternal() = jobService.add(Option(user), FeedbackReportJob(department, startDate, endDate))

	override def describe(d: Description) = d.department(department)

	override def validate(errors: Errors) {
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "startDate", "feedback.report.emptyDate")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "endDate", "feedback.report.emptyDate")
		if(endDate.isBefore(startDate)) {
			errors.rejectValue("startDate", "feedback.report.dateRange")
		}
	}

}