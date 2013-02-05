package uk.ac.warwick.tabula.profiles.web.controllers.tutor

import uk.ac.warwick.tabula.web.controllers.BaseController
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.ModelAttribute
import uk.ac.warwick.tabula.web.Mav
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.stereotype.Controller
import javax.validation.Valid
import uk.ac.warwick.tabula.profiles.web.ProfileBreadcrumbs
import uk.ac.warwick.tabula.data.model.Member
import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.profiles.commands.tutor.TutorSearchProfilesCommand
import org.springframework.web.bind.annotation.RequestParam

@Controller
class TutorSearchController extends TutorProfilesController {
	
	@ModelAttribute("tutorSearchProfilesCommand") def tutorSearchProfilesCommand = new TutorSearchProfilesCommand(user)
	
	@RequestMapping(value=Array("/tutor/tutor_search"), params=Array("!query"))
	def form(@ModelAttribute cmd: TutorSearchProfilesCommand) = Mav("tutor/tutor_form")
	
	@RequestMapping(value=Array("/tutor/tutor_search"), params=Array("query"))
	def submit(@Valid @ModelAttribute cmd: TutorSearchProfilesCommand, errors: Errors, @RequestParam("studentUniId") studentUniId: String) = {
		
		if (errors.hasErrors) {
			form(cmd)
		} else {
			Mav("tutor/tutor_results",
				"studentUniId" -> studentUniId,
				"results" -> cmd.apply())
		}
	}
	
	@RequestMapping(value=Array("/tutor.json"), params=Array("query"))
	def submitJson(@Valid @ModelAttribute cmd: TutorSearchProfilesCommand, errors: Errors) = {
		if (errors.hasErrors) {
			form(cmd)
		} else {
			val profilesJson: JList[Map[String, Object]] = toJson(cmd.apply())
			
			Mav(new JSONView(profilesJson))
		}
	}
	
	def toJson(profiles: Seq[Member]) = {
		def memberToJson(member: Member) = Map[String, String](
			"name" -> {member.fullName match {
				case None => "[Unknown user]"
				case Some(name) => name
			}},
			"id" -> member.universityId,
			"userId" -> member.userId,
			"description" -> member.description)
			
		profiles.map(memberToJson(_))
	}
}
