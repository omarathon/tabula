package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping
import uk.ac.warwick.tabula.data.model.{StudentMember, StudentRelationship, Department, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.commands.{ViewRelatedStudentsCommand, MissingStudentRelationshipCommand, ViewStudentRelationshipsCommand}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(value = Array("/department/{department}/{relationshipType}"))
class ViewStudentRelationshipsController extends ProfilesController {
	@ModelAttribute("viewStudentRelationshipsCommand") def viewStudentRelationshipsCommand(@PathVariable("department") department: Department, @PathVariable("relationshipType") relationshipType: StudentRelationshipType) =
		new ViewStudentRelationshipsCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute("viewStudentRelationshipsCommand") command: ViewStudentRelationshipsCommand): Mav = {
		val agentGraph = command.apply
		Mav("relationships/agent_view",
			"agentRelationships" -> agentGraph.studentMap,
			"studentCount" -> agentGraph.studentCount,
			"missingCount" -> agentGraph.missingCount,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/department/{department}/{relationshipType}/missing"))
class MissingStudentRelationshipController extends ProfilesController {
	@ModelAttribute("missingStudentRelationshipCommand") def missingStudentRelationshipCommand(@PathVariable("department") department: Department, @PathVariable("relationshipType") relationshipType: StudentRelationshipType) =
		new MissingStudentRelationshipCommand(department, relationshipType)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute("missingStudentRelationshipCommand") missing: MissingStudentRelationshipCommand): Mav = {
		val (studentCount, missingStudents) = missing.apply
		Mav("relationships/missing_agent_view",
			"studentCount" -> studentCount,
			"missingStudents" -> missingStudents,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/{relationshipType}/students"))
class ViewStudentRelationshipStudentsController extends ProfilesController {
	@ModelAttribute("viewRelatedStudentsCommand") def command(@PathVariable("relationshipType") relationshipType: StudentRelationshipType) =
		ViewRelatedStudentsCommand(currentMember, relationshipType)

	@RequestMapping
	def view(@ModelAttribute("viewRelatedStudentsCommand") viewRelatedStudentsCommand: Appliable[Seq[StudentMember]]): Mav = {
		if(ajax) Mav("relationships/student_view_results", "students" -> viewRelatedStudentsCommand.apply).noLayout()
		else Mav("relationships/student_view", "students" -> viewRelatedStudentsCommand.apply)
	}
}
