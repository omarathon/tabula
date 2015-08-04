package uk.ac.warwick.tabula.api.web.controllers.coursework.assignments

import javax.validation.Valid

import org.springframework.http.MediaType
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.WebDataBinder
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.api.web.helpers._
import uk.ac.warwick.tabula.web.Routes
import uk.ac.warwick.tabula.{CurrentUser, DateFormats, WorkflowStageHealth}
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.coursework.commands.assignments.{DeleteAssignmentCommand, EditAssignmentCommand, SubmissionAndFeedbackCommand}
import uk.ac.warwick.tabula.coursework.helpers.{CourseworkFilters, CourseworkFilter}
import uk.ac.warwick.tabula.data.model.forms.ExtensionState
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}
import uk.ac.warwick.util.web.bind.AbstractPropertyEditor
import uk.ac.warwick.tabula.JavaImports._

import scala.util.Try

@Controller
@RequestMapping(Array("/v1/module/{module}/assignments/{assignment}"))
class AssignmentController extends ApiController
	with GetAssignmentApi
	with EditAssignmentApi
	with DeleteAssignmentApi
	with AssignmentToJsonConverter
	with AssessmentMembershipInfoToJsonConverter
	with AssignmentStudentToJsonConverter
	with ReplacingAssignmentStudentMessageResolver {
	validatesSelf[SelfValidating]
}

trait GetAssignmentApi {
	self: ApiController with AssignmentToJsonConverter with AssignmentStudentToJsonConverter =>

	@ModelAttribute("getCommand")
	def getCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults] =
		SubmissionAndFeedbackCommand(module, assignment)

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def get(@Valid @ModelAttribute("getCommand") command: Appliable[SubmissionAndFeedbackCommand.SubmissionAndFeedbackResults], errors: Errors, @PathVariable assignment: Assignment) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val results = command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok",
				"assignment" -> jsonAssignmentObject(assignment),
				"genericFeedback" -> assignment.genericFeedback,
				"students" -> results.students.map(jsonAssignmentStudentObject)
			)))
		}
	}

	@InitBinder(Array("getCommand"))
	def getBinding(binder: WebDataBinder) {
		binder.registerCustomEditor(classOf[CourseworkFilter], new AbstractPropertyEditor[CourseworkFilter] {
			override def fromString(name: String) = CourseworkFilters.of(name)
			override def toString(filter: CourseworkFilter) = filter.getName
		})
	}

}

trait EditAssignmentApi {
	self: ApiController with AssignmentToJsonConverter with AssignmentStudentToJsonConverter with GetAssignmentApi =>

	@ModelAttribute("editCommand")
	def editCommand(@PathVariable module: Module, @PathVariable assignment: Assignment, user: CurrentUser): EditAssignmentCommand =
		new EditAssignmentCommand(module, assignment, user)

	@RequestMapping(method = Array(PUT), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
	def edit(@RequestBody request: EditAssignmentRequest, @ModelAttribute("editCommand") command: EditAssignmentCommand, errors: Errors) = {
		request.copyTo(command, errors)

		globalValidator.validate(command, errors)
		command.validate(errors)
		command.afterBind()

		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			val assignment = command.apply()

			// Return the GET representation
			get(getCommand(assignment.module, assignment), errors, assignment)
		}
	}
}

class EditAssignmentRequest extends AssignmentPropertiesRequest[EditAssignmentCommand] {

	// set defaults to null
	openEnded = null
	collectMarks = null
	collectSubmissions = null
	restrictSubmissions = null
	allowLateSubmissions = null
	allowResubmission = null
	displayPlagiarismNotice = null
	allowExtensions = null
	summative = null
	dissertation = null
	includeInFeedbackReportWithoutSubmissions = null
	automaticallyReleaseToMarkers = null
	automaticallySubmitToTurnitin = null

}

trait DeleteAssignmentApi {
	self: ApiController =>

	@ModelAttribute("deleteCommand")
	def deleteCommand(@PathVariable module: Module, @PathVariable assignment: Assignment): DeleteAssignmentCommand = {
		val command = new DeleteAssignmentCommand(module, assignment)
		command.confirm = true
		command
	}

	@RequestMapping(method = Array(DELETE), produces = Array("application/json"))
	def delete(@Valid @ModelAttribute("deleteCommand") command: DeleteAssignmentCommand, errors: Errors) = {
		if (errors.hasErrors) {
			Mav(new JSONErrorView(errors))
		} else {
			command.apply()

			Mav(new JSONView(Map(
				"success" -> true,
				"status" -> "ok"
			)))
		}
	}
}
