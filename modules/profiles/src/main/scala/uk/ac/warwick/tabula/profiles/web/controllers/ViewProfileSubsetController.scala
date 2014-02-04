package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.commands.Appliable

import uk.ac.warwick.tabula.profiles.commands.{ProfileSubset, ViewProfileSubsetCommand}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.spring.Wire

@Controller
@RequestMapping(Array("/view/subset/{student}"))
class ViewProfileSubsetController extends ProfilesController {

	var userLookup = Wire[UserLookupService]

	@ModelAttribute("command")
	def getViewProfileSubsetCommand(@PathVariable student: String) =
		ViewProfileSubsetCommand(student, profileService, userLookup)

	@RequestMapping
	def viewProfile(@ModelAttribute("command") command: Appliable[ProfileSubset]) = {

		val profileSubset = command.apply()

		Mav("profile/view_subset",
				"isMember" -> profileSubset.isMember,
				"studentUser" -> profileSubset.user,
				"profile" -> profileSubset.profile,
				"studentCourseDetails" -> profileSubset.courseDetails
		).noLayout()
	}

}