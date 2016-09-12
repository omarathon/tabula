package uk.ac.warwick.tabula.web.controllers.coursework.admin

import uk.ac.warwick.tabula.web.controllers.coursework.OldCourseworkController
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Profile
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.UserLookupService

class SubmissionReportCommand(val module: Module, val assignment: Assignment) extends Command[SubmissionsReport] with ReadOnly with Unaudited {

	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read, assignment)

	def applyInternal() = assignment.submissionsReport

}

@Profile(Array("cm1Enabled")) @Controller
@RequestMapping(Array("/coursework/admin/module/{module}/assignments/{assignment}/submissions-report"))
class SubmissionReportController extends OldCourseworkController {

	@Autowired var features: Features = _
	@Autowired var userLookup: UserLookupService = _

	@ModelAttribute def command(@PathVariable module:Module, @PathVariable assignment: Assignment) =
		new SubmissionReportCommand(module, assignment)

	@RequestMapping
	def get(command: SubmissionReportCommand): Mav = {
		val report = command.apply()

		val submissionOnly = usersByWarwickIds(report.submissionOnly.toList)
		val feedbackOnly = usersByWarwickIds(report.feedbackOnly.toList)
		val hasNoAttachments = usersByWarwickIds(report.withoutAttachments.toList)
		val hasNoMarks = usersByWarwickIds(report.withoutMarks.toList)
		val plagiarised = usersByWarwickIds(report.plagiarised.toList)

		Mav("coursework/admin/assignments/submissionsreport",
			"assignment" -> command.assignment,
			"submissionOnly" -> submissionOnly,
			"feedbackOnly" -> feedbackOnly,
			"hasNoAttachments" -> hasNoAttachments,
			"hasNoMarks" -> hasNoMarks,
			"plagiarised" -> plagiarised,
			"report" -> report).noLayoutIf(ajax)
	}

	def usersByWarwickIds(ids: Seq[String]) = userLookup.getUsersByWarwickUniIds(ids).values.toSeq.sortBy { _.getWarwickId }

	def surname(user: User) = user.getLastName

}
