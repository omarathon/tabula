package uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti

import org.springframework.stereotype.Controller
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.{PathVariable, RequestBody, ModelAttribute, RequestMapping}
import com.fasterxml.jackson.annotation.{JsonIgnoreProperties, JsonAutoDetect}
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.api.commands.JsonApiRequest
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.commands._
import scala.beans.BeanProperty
import uk.ac.warwick.tabula.helpers.Logging
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services.{AssessmentServiceComponent, AutowiringAssessmentServiceComponent}
import uk.ac.warwick.tabula.services.turnitinlti.TurnitinLtiService
import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.web.views.{JSONView, JSONErrorView}
import uk.ac.warwick.tabula.api.web.controllers.coursework.turnitinlti.TurnitinLtiSubmitAssignmentResponseController.TurnitinLtiSubmitAssignmentResponseCommand


/**
 * We expect a response from Turnitin in the format:
 *
  {"resource_link_id":"Assignment-8aa6aecf-48e3-4ce3-9c5e-dd12f7b520e8",
   "resource_tool_placement_url":"https://sandbox.turnitin.com/api/lti/1p0/resource_tool_data/13071114?lang=en_us",
   "assignmentid":13071114}
 *
 */

object TurnitinLtiSubmitAssignmentResponseController {
		type TurnitinLtiSubmitAssignmentResponseCommand = Appliable[Unit]
			with TurnitinLtiSubmitAssignmentResponseRequestState with SelfValidating
}

@Controller
@RequestMapping(Array("/v1/turnitin/turnitin-submit-assignment-response/assignment/{assignment}"))
class TurnitinLtiSubmitAssignmentResponseController extends ApiController with TurnitinLtiSubmitAssignmentResponseApi

trait TurnitinLtiSubmitAssignmentResponseApi {
	self: ApiController =>

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable assignment: Assignment): TurnitinLtiSubmitAssignmentResponseCommand =
		TurnitinLtiSubmitAssignmentResponseCommand(assignment)

@RequestMapping(method=Array(POST), consumes = Array(MediaType.APPLICATION_JSON_VALUE), produces = Array("application/json"))
def inspectResponse(@RequestBody req: TurnitinLtiSubmitAssignmentResponseRequest,
										@ModelAttribute("command") command: TurnitinLtiSubmitAssignmentResponseCommand,
										@PathVariable assignment: Assignment,
										errors: Errors) {
			req.copyTo(command, errors)
			command.validate(errors)
			if (errors.hasErrors) {
				Mav(new JSONErrorView(errors))
			} else {
				command.apply()
				Mav(new JSONView(Map(
					"success" -> true,
					"assignment" -> assignment.id,
					"turnitinAssignment" -> assignment.turnitinId
				)))
			}
		}
}

@JsonAutoDetect
@JsonIgnoreProperties(ignoreUnknown = true)
class TurnitinLtiSubmitAssignmentResponseRequest extends JsonApiRequest[TurnitinLtiSubmitAssignmentResponseRequestState] {

	@BeanProperty var assignmentid: String = _
	@BeanProperty var resource_link_id: String = _

	override def copyTo(state: TurnitinLtiSubmitAssignmentResponseRequestState, errors: Errors) {
		state.assignmentid = assignmentid
		state.resource_link_id = resource_link_id
	}
}

object TurnitinLtiSubmitAssignmentResponseCommand {
	def apply(assignment: Assignment) = new TurnitinLtiSubmitAssignmentResponseCommandInternal(assignment)
		with ComposableCommand[Unit]
		with TurnitinLtiSubmitAssignmentResponseCommandDescription
		with PubliclyVisiblePermissions
		with AutowiringAssessmentServiceComponent
		with TurnitinLtiSubmitAssignmentResponseValidation

}

class TurnitinLtiSubmitAssignmentResponseCommandInternal(val assignment: Assignment) extends CommandInternal[Unit]
with TurnitinLtiSubmitAssignmentResponseCommandState with Logging  {
	self: AssessmentServiceComponent =>

	override protected def applyInternal() = {
		assignment.turnitinId = assignmentid
		assessmentService.save(assignment)
	}
}

trait TurnitinLtiSubmitAssignmentResponseRequestState {

	var assignmentid: String = _
	var resource_link_id: String = _
}

trait TurnitinLtiSubmitAssignmentResponseCommandState extends TurnitinLtiSubmitAssignmentResponseRequestState {
	def assignment: Assignment
}

trait TurnitinLtiSubmitAssignmentResponseValidation extends SelfValidating {
	self: TurnitinLtiSubmitAssignmentResponseCommandState with AssessmentServiceComponent =>
	override def validate(errors: Errors) = {
		if (assignment != assessmentService.getAssignmentById(resource_link_id.substring(TurnitinLtiService.AssignmentPrefix.length)).get) {
			errors.rejectValue("assignment", "turnitin.assignment.invalid")
		}
	}

}

trait TurnitinLtiSubmitAssignmentResponseCommandDescription extends Describable[Unit] {
	self: TurnitinLtiSubmitAssignmentResponseCommandState =>

	override lazy val eventName = "TurnitinLtiSubmitAssignmentResponse"

	def describe(d: Description) {
		d.assignment(assignment)
		d.property("existing turnitin id", assignment.turnitinId)
		d.property("new turnitin id", assignmentid)
	}

}
