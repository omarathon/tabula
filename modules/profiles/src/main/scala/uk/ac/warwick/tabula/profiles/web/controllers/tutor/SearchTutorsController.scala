package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import javax.validation.Valid
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.profiles.commands.SearchTutorsCommand
import uk.ac.warwick.tabula.profiles.web.controllers.ProfilesController

@Controller
class SearchTutorsController extends ProfilesController {
	
	@ModelAttribute("searchTutorsCommand") def searchTutorsCommand = new SearchTutorsCommand(user)
		
	@ModelAttribute("tutorToDisplay") def tutorToDisplay(@ModelAttribute("student") student: Member) =  profileService.getPersonalTutor(student)

	@RequestMapping(value=Array("/tutor/search"), params=Array("!query"))
	def form(@ModelAttribute cmd: SearchTutorsCommand) = Mav("tutor/edit/view", "displayOptionToSave" -> false)

	@RequestMapping(value=Array("/tutor/search"), params=Array("query"))
	def submit(@Valid @ModelAttribute("searchTutorsCommand") cmd: SearchTutorsCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("tutor/edit/results",
				"results" -> cmd.apply())
		}
	}
/*
	@RequestMapping(value=Array("/tutor/search.json"), params=Array("query"))
	def submitJson(@Valid @ModelAttribute cmd: searchTutorsCommand, errors: Errors) = {
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
