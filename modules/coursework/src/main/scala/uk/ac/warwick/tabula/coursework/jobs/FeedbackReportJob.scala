package uk.ac.warwick.tabula.coursework.jobs

import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.jobs.{Job, JobPrototype}
import org.joda.time.DateTime
import org.springframework.stereotype.Component
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.web.views.FreemarkerRendering
import uk.ac.warwick.tabula.services.jobs.JobInstance
import uk.ac.warwick.tabula.services.{ModuleAndDepartmentService, AssignmentService}
import uk.ac.warwick.tabula.{CurrentUser, DateFormats}
import uk.ac.warwick.spring.Wire
import org.springframework.mail.javamail.{MimeMessageHelper, MimeMailMessage}
import javax.annotation.Resource
import uk.ac.warwick.util.mail.WarwickMailSender
import org.springframework.beans.factory.annotation.Value
import java.io.ByteArrayOutputStream
import org.springframework.core.io.ByteArrayResource
import freemarker.template.Configuration
import uk.ac.warwick.tabula.coursework.services.feedbackreport.FeedbackReport

object FeedbackReportJob {
	val identifier = "feedback-report"
	def apply(department: Department, start: DateTime, end: DateTime) = JobPrototype(identifier, Map(
		"department" -> department.id ,
		"startDate" -> DateFormats.FeedbackReportDate.print(start),
		"endDate" -> DateFormats.FeedbackReportDate.print(end)
	))
}

@Component
class FeedbackReportJob extends Job with Logging with FreemarkerRendering {

	val identifier = FeedbackReportJob.identifier

	implicit var freemarker: Configuration = Wire.auto[Configuration]
	var departmentService: ModuleAndDepartmentService = Wire.auto[ModuleAndDepartmentService]
	var assignmentService = Wire.auto[AssignmentService]

	@Resource(name = "mailSender") var mailer: WarwickMailSender = _

	@Value("${mail.noreply.to}") var replyAddress: String = _
	val excelContentType = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"
	val WaitingRetries = 50
	val WaitingSleep = 20000
	var sendEmails = true

	def run(implicit job: JobInstance) {
		new Runner(job).run()
	}

	def renderJobDoneEmailText(user: CurrentUser, department: Department, startDate: DateTime, endDate: DateTime) = {
		renderToString("/WEB-INF/freemarker/emails/feedback_report_done.ftl", Map(
			"department" -> department,
			"startDate" -> startDate,
			"endDate" -> endDate,
			"user" -> user
		))
	}

	// Job is a shared service, so rather than pass objects between methods,
	// let's use an inner class.
	class Runner(job: JobInstance) {
		implicit private val _job: JobInstance = job

		val department = {
			val id = job.getString("department")
			departmentService.getDepartmentById(id) getOrElse {
				updateStatus("%s is not a valid department ID" format (id))
				throw obsoleteJob
			}
		}

		val startDate = {
			val stringDate = job.getString("startDate")
			try {
				DateFormats.FeedbackReportDate.parseDateTime(stringDate)
			} catch {
				case e:IllegalArgumentException => {
					updateStatus("Start date format was incorrect")
					throw obsoleteJob
				}
			}
		}
		val defaultStartDate = new DateTime().minusMonths(3)

		val endDate = {
			val stringDate = job.getString("endDate")
			try {
				DateFormats.FeedbackReportDate.parseDateTime(stringDate)
			} catch {
				case e:IllegalArgumentException => {
					updateStatus("End date format was incorrect")
					throw obsoleteJob
				}
			}
		}
		val defaultEndDate = new DateTime()

		def run() {

			val report = new FeedbackReport(department, startDate, endDate)

			val assignmentSheet = report.generateAssignmentSheet(department)
			val moduleSheet = report.generateModuleSheet(department)

			report.buildAssignmentData()

			report.populateAssignmentSheet(assignmentSheet)
			report.populateModuleSheet(moduleSheet)

			report.formatWorksheet(assignmentSheet, report.assignmentSheetSize)
			report.formatWorksheet(moduleSheet, report.moduleSheetSize)

			updateStatus("Generated a report.")
			job.succeeded = true

			// Adapter stuff to go from XSSF to the writer without having to save the spreadsheet to disk
			val out = new ByteArrayOutputStream()
			report.workbook.write(out)
			out.close()
			val in = new ByteArrayResource(out.toByteArray)

			if (sendEmails) {
				debug("Sending an email to " + job.user.email)
				val mime = mailer.createMimeMessage()

				val helper = new MimeMessageHelper(mime, true)
				helper.setFrom(replyAddress)
				helper.setTo(job.user.email)
				helper.setSubject("Feedback report generated for %s" format (department.name))
				helper.setText(renderJobDoneEmailText(job.user, department, startDate, endDate))
				helper.addAttachment("Feedback Report.xlsx", in, excelContentType)

				mailer.send(mime)
			}
		}
	}

}
