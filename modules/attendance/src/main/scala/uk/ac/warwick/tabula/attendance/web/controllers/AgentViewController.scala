package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestParam, RequestMapping}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.{GroupedMonitoringPoint, StudentPointsData, AgentViewCommand}

@Controller
@RequestMapping(Array("/agent/{relationshipType}"))
class AgentViewController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @RequestParam(value="academicYear", required = false) academicYear: AcademicYear) =
		AgentViewCommand(currentMember, relationshipType, Option(academicYear))

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[(Seq[StudentPointsData], Map[String, Seq[GroupedMonitoringPoint]])]) = {
		val data = cmd.apply()
		Mav("agent/home",
			"students" -> data._1,
			"necessaryTerms" -> data._1.flatMap{ data => data.pointsByTerm.keySet }.distinct,
			"groupedPoints" -> data._2,
			"department" -> currentMember.homeDepartment
		)
	}

}
