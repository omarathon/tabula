package uk.ac.warwick.tabula.attendance.web.controllers.agent.old

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.attendance.commands.agent.old.AgentPointRecordCommand
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceState, MonitoringCheckpoint, MonitoringPoint}
import uk.ac.warwick.tabula.web.Mav

@RequestMapping(Array("/agent/{relationshipType}/2013/point/{point}/record"))
@Controller
class OldAgentPointRecordController extends AttendanceController {

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
			"returnTo" -> getReturnTo(Routes.old.agent.view(relationshipType))
		).crumbs(Breadcrumbs.Old.Agent(relationshipType))
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
			Redirect(Routes.old.agent.view(relationshipType))
		}
	}

}