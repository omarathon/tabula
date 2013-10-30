package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationshipType, Member}
import uk.ac.warwick.tabula.commands.{Appliable, CommandInternal, Unaudited, ReadOnly, ComposableCommand}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringMonitoringPointServiceComponent, MonitoringPointServiceComponent, AutowiringRelationshipServiceComponent, RelationshipServiceComponent}
import uk.ac.warwick.tabula.AcademicYear
import org.joda.time.DateTime
import org.springframework.web.bind.annotation

object AgentViewCommand {
	def apply(agent: Member, relationshipType: StudentRelationshipType, academicYearOption: Option[AcademicYear]) =
		new AgentViewCommand(agent, relationshipType, academicYearOption)
		with ComposableCommand[Seq[(StudentMember, Int)]]
		with AgentViewCommandPermissions
		with ReadOnly with Unaudited
		with AutowiringRelationshipServiceComponent
		with AutowiringMonitoringPointServiceComponent
}

class AgentViewCommand(val agent: Member, val relationshipType: StudentRelationshipType, val academicYearOption: Option[AcademicYear])
	extends CommandInternal[Seq[(StudentMember, Int)]] with AgentViewCommandState {

	this: RelationshipServiceComponent with MonitoringPointServiceComponent =>

	def applyInternal() = {
		val f = relationshipService.listStudentRelationshipsWithMember(relationshipType, agent).flatMap(_.studentMember)
		f.map{student =>
			student -> monitoringPointService.countMissedPoints(student, academicYear)
		}
	}
}

trait AgentViewCommandPermissions extends RequiresPermissionsChecking {
	this: AgentViewCommandState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Profiles.StudentRelationship.Read(p.mandatory(relationshipType)), agent)
	}
}

trait AgentViewCommandState {
	def agent: Member
	def relationshipType: StudentRelationshipType
	def academicYearOption: Option[AcademicYear]
	val thisAcademicYear = AcademicYear.guessByDate(DateTime.now())
	val academicYear = academicYearOption.getOrElse(thisAcademicYear)

}

@Controller
@RequestMapping(Array("/agent/{relationshipType}"))
class AgentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @RequestParam(value="academicYear", required = false) academicYear: AcademicYear) =
		AgentViewCommand(currentMember, relationshipType, Option(academicYear))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Seq[(StudentMember, Int)]]) = {
		Mav("agent/home", "students" -> cmd.apply())
	}

}
