package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments.submissions

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestMethod}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.SubmissionToJsonConverter
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.cm2.assignments.AdminGetSingleSubmissionCommand
import uk.ac.warwick.tabula.commands.coursework.assignments.SubmissionAndFeedbackCommand
import uk.ac.warwick.tabula.data.model.{Assignment, Module, Submission}
import uk.ac.warwick.tabula.services.fileserver.RenderableFile
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}

abstract class AssignmentSubmissionsController extends ApiController
	with SubmissionToJsonConverter

@Controller
@RequestMapping(
	method = Array(RequestMethod.GET),
	value = Array("/v1/module/{module}/assignments/{assignment}/submissions")
)
class GetAssignmentSubmissionsController extends AssignmentSubmissionsController
	with ListSubmissionsForAssignmentApi

trait ListSubmissionsForAssignmentApi {
	self: AssignmentSubmissionsController =>

	@ModelAttribute("listCommand")
	def listCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def list(@Valid @ModelAttribute("listCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"submissions" -> results.students.map { student =>
					student.user.getWarwickId.maybeText.getOrElse(student.user.getUserId) -> jsonSubmissionObject(student)
				}.toMap
			)))
		}
	}
}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}/submissions/{submission}"))
class AssignmentSubmissionController extends AssignmentSubmissionsController
	with GetSubmissionApi

trait GetSubmissionApi {
	self: AssignmentSubmissionsController =>

	@ModelAttribute("listCommand")
	def getCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def get(@Valid @ModelAttribute("listCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment, @PathVariable submission: Submission): Mav = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val result = mandatory(command.apply().students.find(_.coursework.enhancedSubmission.exists(_.submission == mandatory(submission))))

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"submission" -> jsonSubmissionObject(result)
			)))
		}
	}
}

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}/submissions/{submission}/{filename}"))
class AssignmentSubmissionDownloadFileController extends ApiController
	with DownloadSingleSubmissionAttachmentApi

trait DownloadSingleSubmissionAttachmentApi {
	self: ApiController =>

	@ModelAttribute("downloadAttachmentCommand")
	def downloadAttachmentCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, @PathVariable submission: Submission, @PathVariable filename: String): AdminGetSingleSubmissionCommand.Command = {
		mustBeLinked(mandatory(assignment), mandatory(module))
		AdminGetSingleSubmissionCommand.single(mandatory(assignment), mandatory(submission), filename)
	}

	@RequestMapping(method = Array(GET))
	def download(@ModelAttribute("downloadAttachmentCommand") command: AdminGetSingleSubmissionCommand.Command): RenderableFile =
		command.apply()
}