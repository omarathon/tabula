package uk.ac.warwick.tabula.profiles.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestMapping, RequestParam, PathVariable, ModelAttribute}
import uk.ac.warwick.tabula.data.model.{StudentCourseDetails, StudentMember, Member}
import uk.ac.warwick.tabula.ItemNotFoundException
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.{ProfileService, UserLookupService}
import uk.ac.warwick.userlookup.User

@Controller
@RequestMapping(Array("/view/subset"))
class ViewProfileSubsetController extends ProfilesController {

	var userLookup = Wire[UserLookupService]

	@RequestMapping(Array("/{student}"))
	def viewProfile(@PathVariable("student") student: String) = {

		val member = profileService.getMemberByUniversityId(student)
		member match {
			case Some(student: StudentMember) => {
				val profiledStudentMember = new ViewProfileCommand(user, student).apply()

				Mav("profile/view_subset",
					"profile" -> profiledStudentMember,
					"viewer" -> currentMember,
					"studentCourseDetails" -> profiledStudentMember.mostSignificantCourseDetails)
					.noLayout()
			}
			case _ => {
				val studentUser = Option(userLookup.getUserByWarwickUniId(student)).getOrElse(throw new ItemNotFoundException())
				Mav("profile/view_nonmember_subset", "studentUser" -> studentUser).noLayout()
			}
		}


	}

}
