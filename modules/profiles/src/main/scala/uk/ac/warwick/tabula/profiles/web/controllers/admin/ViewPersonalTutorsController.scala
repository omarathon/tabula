package uk.ac.warwick.tabula.profiles.web.controllers.admin

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.RequestMapping

import uk.ac.warwick.tabula.data.model.{StudentRelationship, RelationshipType, Department}
import uk.ac.warwick.tabula.profiles.commands.{ViewRelatedStudentsCommand, MissingPersonalTutorsCommand, ViewPersonalTutorsCommand}
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Appliable

@Controller
@RequestMapping(value = Array("/department/{department}/tutors"))
class ViewPersonalTutorsController extends ProfilesController {
	@ModelAttribute("viewPersonalTutorsCommand") def viewPersonalTutorsCommand(@PathVariable("department") department: Department) =
		new ViewPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute("viewPersonalTutorsCommand") command: ViewPersonalTutorsCommand): Mav = {
		val tutorGraph = command.apply
		Mav("tutors/tutor_view",
			"tutorRelationships" -> tutorGraph.tuteeMap,
			"studentCount" -> tutorGraph.studentCount,
			"missingCount" -> tutorGraph.missingCount,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/department/{department}/tutors/missing"))
class MissingPersonalTutorsController extends ProfilesController {
	@ModelAttribute("missingPersonalTutorsCommand") def missingPersonalTutorsCommand(@PathVariable("department") department: Department) =
		new MissingPersonalTutorsCommand(department)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@PathVariable("department") department: Department, @ModelAttribute("missingPersonalTutorsCommand") missing: MissingPersonalTutorsCommand): Mav = {
		val (studentCount, missingStudents) = missing.apply
		Mav("tutors/missing_tutor_view",
			"studentCount" -> studentCount,
			"missingStudents" -> missingStudents,
			"department" -> department
		)
	}
}

@Controller
@RequestMapping(value = Array("/tutees"))
class ViewPersonalTuteesController extends ProfilesController {
	@ModelAttribute("cmd") def command = ViewRelatedStudentsCommand(currentMember, RelationshipType.PersonalTutor)

	@RequestMapping(method = Array(HEAD, GET))
	def view(@ModelAttribute("cmd") cmd: Appliable[Seq[StudentRelationship]]): Mav = {
		Mav("tutors/tutee_view", "tutees" -> cmd.apply)
	}
}
