package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import scala.Array
import uk.ac.warwick.tabula.attendance.commands.{AgentPointRecordCommand, SetMonitoringCheckpointCommand}
import uk.ac.warwick.tabula.data.model.attendance.{MonitoringCheckpoint, AttendanceState, MonitoringPoint}
import uk.ac.warwick.tabula.web.Mav
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.{PopulateOnForm, Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType

@RequestMapping(Array("/agent/{relationshipType}/point/{point}/record"))
@Controller
class AgentPointRecordController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @PathVariable point: MonitoringPoint) = {
		AgentPointRecordCommand(currentMember, user, relationshipType, point)
	}


	@RequestMapping(method = Array(GET, HEAD))
	def list(
		@ModelAttribute("command") command: Appliable[Seq[MonitoringCheckpoint]] with PopulateOnForm,
		@PathVariable relationshipType: StudentRelationshipType
	): Mav = {
		command.populate()
		form(command, relationshipType)
	}


	def form(@ModelAttribute command: Appliable[Seq[MonitoringCheckpoint]] with PopulateOnForm, relationshipType: StudentRelationshipType): Mav = {
		Mav("home/record_point",
			"allCheckpointStates" -> AttendanceState.values,
			"returnTo" -> getReturnTo(Routes.agent.view(relationshipType))
		).crumbs(Breadcrumbs.Agent(relationshipType))
	}


	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") command: Appliable[Seq[MonitoringCheckpoint]] with PopulateOnForm,
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType
	) = {
		if(errors.hasErrors) {
			form(command, relationshipType)
		} else {
			command.apply()
			Redirect(Routes.agent.view(relationshipType))
		}
	}

}