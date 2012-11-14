package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.BaseController
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.coursework.data.model._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.coursework.actions.Participate
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.coursework.Features

class SubmissionReportCommand {
	@BeanProperty var assignment: Assignment = _
	@BeanProperty var module: Module = _
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/submissions-report"))
class SubmissionReportController extends BaseController {

	@Autowired var features: Features = _
	@Autowired var userLookup: UserLookupInterface = _

	@RequestMapping()
	def get(command: SubmissionReportCommand): Mav = {
		mustBeLinked(command.assignment, command.module)
		mustBeAbleTo(Participate(command.module))

		val report = command.assignment.submissionsReport
		val submissionOnly = report.submissionOnly.toList.sorted.map { userByWarwickId }
		val feedbackOnly = report.feedbackOnly.toList.sorted.map { userByWarwickId }
		val hasNoAttachments = report.withoutAttachments.toList.sorted.map { userByWarwickId }
		val hasNoMarks = report.withoutMarks.toList.sorted.map { userByWarwickId }
		val plagiarised = report.plagiarised.toList.sorted.map { userByWarwickId }

		Mav("admin/assignments/submissionsreport",
			"assignment" -> command.assignment,
			"submissionOnly" -> submissionOnly,
			"feedbackOnly" -> feedbackOnly,
			"hasNoAttachments" -> hasNoAttachments,
			"hasNoMarks" -> hasNoMarks,
			"plagiarised" -> plagiarised,
			"report" -> report).noLayoutIf(ajax)
	}

	def userByWarwickId(id: String) = userLookup.getUserByWarwickUniId(id)

	def surname(user: User) = user.getLastName

}
