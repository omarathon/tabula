package uk.ac.warwick.tabula.web.controllers.cm2.admin.assignments

import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.cm2.assignments.ViewAssignmentAuditLogCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services.cm2.AssignmentAuditEvent
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(value = Array("/${cm2.prefix}/admin/assignments/{assignment}/audit"))
class AssignmentAuditController extends CourseworkController {

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): Appliable[Seq[AssignmentAuditEvent]] = {
		notDeleted(assignment)
		ViewAssignmentAuditLogCommand(mandatory(assignment))
	}

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable assignment: Assignment, @ModelAttribute("command") cmd: Appliable[Seq[AssignmentAuditEvent]]): Mav = {
		val auditEvents = cmd.apply()

		Mav("cm2/admin/assignments/assignment_audit",
			"module" -> assignment.module,
			"assignment" -> assignment,
			"auditEvents" -> auditEvents)
			.crumbsList(Breadcrumbs.assignment(assignment))
	}

}

