package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType}
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.data.model.attendance.MonitoringCheckpoint
import uk.ac.warwick.tabula.attendance.commands.{AgentStudentRecordCommandState, AgentStudentRecordCommand}
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/agent/{relationshipType}/{student}/record"))
class AgentStudentRecordController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable student: StudentMember,
		@RequestParam(value="academicYear", required = false) academicYear: AcademicYear
	) = AgentStudentRecordCommand(currentMember, relationshipType, student, Option(academicYear))

	@RequestMapping(method = Array(GET, HEAD))
	def list(@ModelAttribute("command") cmd: AgentStudentRecordCommand) = {
		cmd.populate()
		form(cmd)
	}

	def form(cmd: Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState) = {
		Mav("home/record_student",
			"returnTo" -> getReturnTo(Routes.agent.student(cmd.student, cmd.relationshipType))
		).crumbs(Breadcrumbs.Agent(cmd.relationshipType), Breadcrumbs.AgentStudent(cmd.student, cmd.relationshipType))
	}

	@RequestMapping(method = Array(POST))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[MonitoringCheckpoint]] with AgentStudentRecordCommandState, errors: Errors) = {
		if(errors.hasErrors) {
			form(cmd)
		} else {
			cmd.apply()
			Redirect(Routes.agent.student(cmd.student, cmd.relationshipType))
		}
	}

}
