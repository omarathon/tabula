package uk.ac.warwick.tabula.coursework.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.coursework.assignments.{FeedbackAuditData, FeedbackAuditCommand}
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model.{MarkingMethod, Assignment}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/admin/module/{module}/assignments/{assignment}/audit/{student}"))
class FeedbackAuditController extends CourseworkController {

	@ModelAttribute("auditCommand")
	def listCommand(@PathVariable assignment: Assignment, @PathVariable student: User) =
		FeedbackAuditCommand(assignment, student)

	@RequestMapping(method=Array(GET))
	def list(@PathVariable assignment: Assignment,
					 @PathVariable student: User,
					 @ModelAttribute("auditCommand") auditCommand: Appliable[FeedbackAuditData]
						) = {
		val auditData = auditCommand.apply()
		Mav("admin/assignments/feedback_audit",
			"command" -> auditCommand,
			"auditData" -> auditData,
			"assignment" -> assignment,
			"isModerated" -> Option(assignment.markingWorkflow).exists(_.markingMethod == MarkingMethod.ModeratedMarking),
			"student" -> student
		).crumbs(
				Breadcrumbs.Department(assignment.module.adminDepartment),
				Breadcrumbs.Module(assignment.module)
			)
	}
}