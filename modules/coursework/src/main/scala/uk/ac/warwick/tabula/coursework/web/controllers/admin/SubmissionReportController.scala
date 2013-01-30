package uk.ac.warwick.tabula.coursework.web.controllers.admin

import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import scala.reflect.BeanProperty
import uk.ac.warwick.tabula.data.model._
import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.permissions._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.userlookup.UserLookupInterface
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.Command
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable

class SubmissionReportCommand(val module: Module, val assignment: Assignment) extends Command[SubmissionsReport] with ReadOnly with Unaudited {
	
	mustBeLinked(assignment, module)
	PermissionCheck(Permissions.Submission.Read(), assignment)

	def applyInternal() = assignment.submissionsReport
	
}

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/submissions-report"))
class SubmissionReportController extends CourseworkController {

	@Autowired var features: Features = _
	@Autowired var userLookup: UserLookupInterface = _
	
	@ModelAttribute def command(@PathVariable("module") module:Module, @PathVariable("assignment") assignment: Assignment) =
		new SubmissionReportCommand(module, assignment)

	@RequestMapping
	def get(command: SubmissionReportCommand): Mav = {
		val report = command.apply()
		
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
