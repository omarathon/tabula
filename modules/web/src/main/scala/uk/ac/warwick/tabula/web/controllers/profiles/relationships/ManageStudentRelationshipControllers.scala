package uk.ac.warwick.tabula.web.controllers.profiles.relationships

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.profiles.relationships.EditStudentRelationshipCommand
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Member, StudentCourseDetails, StudentRelationship, StudentRelationshipType}
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

trait ManageStudentRelationshipController extends ProfilesController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable relationshipType: StudentRelationshipType, @PathVariable studentCourseDetails: StudentCourseDetails) =
		EditStudentRelationshipCommand(studentCourseDetails, relationshipType, user)

	def render(agent: Option[Member]): Mav = {
		Mav("profiles/relationships/edit/view", "existingAgent" -> agent).noLayoutIf(ajax)
	}

}

@Controller
@RequestMapping(Array("/profiles/{relationshipType}/{studentCourseDetails}/add"))
class AddStudentRelationshipController extends ManageStudentRelationshipController {

	@RequestMapping(method = Array(GET))
	def form(@ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]]): Mav = {
		render(None)
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]],
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails
	): Mav = {
		if (errors.hasErrors){
			form(cmd)
		} else {
			cmd.apply()

			if (ajax) {
				form(cmd)
			} else {
				Redirect(Routes.Profile.relationshipType(studentCourseDetails.student, relationshipType))
			}

		}
	}

}

@Controller
@RequestMapping(Array("/profiles/{relationshipType}/{studentCourseDetails}/edit/{agent}"))
class EditStudentRelationshipController extends ManageStudentRelationshipController {

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]],
		@PathVariable("agent") agent: Member
	): Mav = {
		render(Some(agent))
	}

	@RequestMapping(method = Array(POST))
	def submit(
		@Valid @ModelAttribute("command") cmd: Appliable[Seq[StudentRelationship]],
		errors: Errors,
		@PathVariable relationshipType: StudentRelationshipType,
		@PathVariable studentCourseDetails: StudentCourseDetails,
		@PathVariable("agent") agent: Member
	): Mav = {
		if (errors.hasErrors){
			form(cmd, agent)
		} else {
			cmd.apply()

			if (ajax) {
				form(cmd, agent)
			} else {
				Redirect(Routes.Profile.relationshipType(studentCourseDetails.student, relationshipType))
			}

		}
	}

}
