package uk.ac.warwick.tabula.web.controllers.coursework.admin

import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.coursework.feedback.SubmissionReportCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.userlookup.User

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/${cm1.prefix}/admin/module/{module}/assignments/{assignment}/submissions-report"))
class OldSubmissionReportController extends OldCourseworkController {

	@Autowired var features: Features = _
	@Autowired var userLookup: UserLookupService = _

	@ModelAttribute def command(@PathVariable module:Module, @PathVariable assignment: Assignment) =
		new SubmissionReportCommand(module, assignment)

	@RequestMapping
	def get(command: SubmissionReportCommand): Mav = {
		val report = command.apply()

		val submissionOnly = usersByUsercodes(report.submissionOnly.toList)
		val feedbackOnly = usersByUsercodes(report.feedbackOnly.toList)
		val hasNoAttachments = usersByUsercodes(report.withoutAttachments.toList)
		val hasNoMarks = usersByUsercodes(report.withoutMarks.toList)
		val plagiarised = usersByUsercodes(report.plagiarised.toList)

		Mav("coursework/admin/assignments/submissionsreport",
			"assignment" -> command.assignment,
			"submissionOnly" -> submissionOnly,
			"feedbackOnly" -> feedbackOnly,
			"hasNoAttachments" -> hasNoAttachments,
			"hasNoMarks" -> hasNoMarks,
			"plagiarised" -> plagiarised,
			"report" -> report).noLayoutIf(ajax)
	}

	def usersByUsercodes(usercodes: Seq[String]): Seq[User] =
		userLookup.getUsersByUserIds(usercodes).values.toSeq.sortBy { u => s"${u.getWarwickId}${u.getUserId}"}

	def surname(user: User): String = user.getLastName

}
