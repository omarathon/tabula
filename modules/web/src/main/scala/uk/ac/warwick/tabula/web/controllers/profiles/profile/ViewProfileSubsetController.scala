package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.profile.{ProfileSubset, ViewProfileSubsetCommand}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.web.controllers.profiles.ProfilesController

@Controller
@RequestMapping(Array("/profiles/view/subset/{student}"))
class ViewProfileSubsetController extends ProfilesController {

	var userLookup = Wire[UserLookupService]

	@ModelAttribute("command")
	def getViewProfileSubsetCommand(@PathVariable student: String) =
		ViewProfileSubsetCommand(student, profileService, userLookup)

	@RequestMapping
	def viewProfile(@ModelAttribute("command") command: Appliable[ProfileSubset]) = {

		val profileSubset = command.apply()

		Mav("profiles/profile/view_subset",
			"isMember" -> profileSubset.isMember,
			"studentUser" -> profileSubset.user,
			"profile" -> profileSubset.profile,
			"studentCourseDetails" -> profileSubset.courseDetails
		).noLayout()
	}

}