package uk.ac.warwick.tabula.web.controllers.attendance.agent.old

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.agent.old.{AgentStudentRecordCommand, AgentStudentRecordCommandState}
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/agent/{relationshipType}/2013/{student}/record"))
class OldAgentStudentRecordController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable student: StudentMember
	) = AgentStudentRecordCommand(currentMember, relationshipType, student, Option(AcademicYear(2013)))

	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState with PopulateOnForm) = {
		cmd.populate()
		form(cmd)
	}

	def form(cmd: Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState) = {
		Mav("attendance/home/record_student",
			"returnTo" -> getReturnTo(Routes.old.agent.student(cmd.student, cmd.relationshipType))
		).crumbs(Breadcrumbs.Old.Agent(cmd.relationshipType), Breadcrumbs.Old.AgentStudent(cmd.student, cmd.relationshipType))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState, errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.old.agent.student(cmd.student, cmd.relationshipType))
		}
	}

}
