package uk.ac.warwick.tabula.coursework.web.controllers.admin


import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.coursework.commands.assignments._
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkController
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.AuditEventIndexService

@Controller
@RequestMapping(value = Array("/admin/module/{module}/assignments/{assignment}/list"))
class SubmissionAndFeedbackController extends CourseworkController {

	var auditIndexService = Wire.auto[AuditEventIndexService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]
	
	@ModelAttribute def command(@PathVariable("module") module: Module, @PathVariable("assignment") assignment: Assignment) = 
		new SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET, HEAD))
	def list(command: SubmissionAndFeedbackCommand) = {
		val (assignment, module) = (command.assignment, command.module)

		command.applyInternal()

		Mav("admin/assignments/submissionsandfeedback/list",
			"assignment" -> assignment,
			"students" -> command.students,
			"awaitingSubmissionExtended" -> command.awaitingSubmissionExtended,
			"awaitingSubmission" -> command.awaitingSubmission,
			"whoDownloaded" -> command.whoDownloaded,
			"stillToDownload" -> command.stillToDownload,
			"hasPublishedFeedback" -> command.hasPublishedFeedback,
			"hasOriginalityReport" -> command.hasOriginalityReport,
			"mustReleaseForMarking" -> command.mustReleaseForMarking)
			.crumbs(Breadcrumbs.Department(module.department), Breadcrumbs.Module(module))
	}

	// Simple object holder
	class Item(val uniId: String, val enhancedSubmission: SubmissionListItem, val feedback: Feedback, val fullName: String) 
	
}
