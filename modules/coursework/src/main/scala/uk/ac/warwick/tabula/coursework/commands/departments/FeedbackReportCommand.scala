package uk.ac.warwick.tabula.coursework.commands.departments

import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.commands.{Description, Command, Unaudited, ReadOnly}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.spring.Wire
import scala.reflect.BeanProperty
import org.joda.time.DateTime
import org.springframework.format.annotation.DateTimeFormat
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.tabula.helpers.SpreadsheetHelper
import uk.ac.warwick.tabula.services.jobs.{JobInstance, JobService}
import uk.ac.warwick.tabula.coursework.jobs.FeedbackReportJob

class FeedbackReportCommand (val department:Department, val user: CurrentUser) extends Command[JobInstance] with ReadOnly with Unaudited with SpreadsheetHelper {
	
	PermissionCheck(Permissions.Department.DownloadFeedbackReport, department)

	@BeanProperty var startDate:DateTime = _
	@BeanProperty var endDate:DateTime = _

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultStartDate = new DateTime().minusMonths(3)

	@DateTimeFormat(pattern = DateFormats.DateTimePicker)
	@BeanProperty
	val defaultEndDate = new DateTime()

	var jobService = Wire.auto[JobService]

	def applyInternal() = jobService.add(Option(user), FeedbackReportJob(department, startDate, endDate))

	override def describe(d: Description) = d.department(department)

}