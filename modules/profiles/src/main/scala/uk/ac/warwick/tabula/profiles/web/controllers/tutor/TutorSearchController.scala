package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.commands.tutor.TutorSearchCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController
import uk.ac.warwick.tabula.data.model.Member

@Controller
class TutorSearchController extends ProfilesController {
	
	@ModelAttribute("tutorSearchCommand") def tutorSearchCommand = new TutorSearchCommand(user)
	
	@ModelAttribute("student") def student(@RequestParam("studentUniId") studentUniId: String) =
		profileService.getMemberByUniversityId(studentUniId).getOrElse(throw new IllegalStateException("Can't find student " + studentUniId))
		
	@ModelAttribute("tutorToDisplay") def tutorToDisplay(@ModelAttribute("student") student: Member) = {
		val currentTutor = profileService.getPersonalTutor(student)
		val currentTutorToDisplay = profileService.getNameAndNumber(currentTutor.getOrElse(throw new IllegalStateException("Can't find membership record for tutor picked")))

		currentTutorToDisplay
	}

	@RequestMapping(value=Array("/tutor/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: TutorSearchCommand) = Mav("tutor/edit/view")

	@RequestMapping(value=Array("/tutor/search"), params=Array("query"))
	def submit(@Valid @ModelAttribute("tutorSearchCommand") cmd: TutorSearchCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("tutor/edit/results",
				"results" -> cmd.apply())
		}
	}
/*
	@RequestMapping(value=Array("/tutor/search.json"), params=Array("query"))
	def submitJson(@Valid @ModelAttribute cmd: tutorSearchCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val profilesJson: JList[Map[String, Object]] = toJson(cmd.apply())
			
			Mav(new JSONView(profilesJson))
		}
	}
	
	def toJson(tutors: Seq[Member]) = {
		def memberToJson(member: Member) = Map[String, String](
			"name" -> {member.fullName match {
				case None => "[Unknown user]"
				case Some(name) => name
			}},
			"id" -> member.universityId,
			"userId" -> member.userId,
			"description" -> member.description)
			
		tutors.map(memberToJson(_))
	}
	* 
	*/
}
