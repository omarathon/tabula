package uk.ac.warwick.tabula.web.controllers.profiles.profile

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.commands.profiles.ProfilesHomeCommand
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.profiles.ProfileBreadcrumbs

@Controller
@RequestMapping(Array("/profiles/view"))
class ViewProfileStudentsController extends AbstractViewProfileController {

	@RequestMapping(Array("/{member}/students"))
	def viewByMemberMapping(
		@PathVariable member: Member,
		@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear]
	): Mav = {
		// We treat this as a masquerade to avoid bypassing permissions checks
		val memberAsCurrentUser = new CurrentUser(
			realUser = user.apparentUser,
			apparentUser = member.asSsoUser,
			profile = Some(member)
		)

		val info = ProfilesHomeCommand(memberAsCurrentUser, Some(member)).apply()
		Mav("profiles/profile/students",
			"relationshipTypesMap" -> info.relationshipTypesMap,
			"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
			"smallGroups" -> info.currentSmallGroups,
			"previousSmallGroups" -> info.previousSmallGroups,
			"isSelf" -> (user.universityId.maybeText.getOrElse("") == member.universityId)
		).crumbs(breadcrumbsStaff(member, ProfileBreadcrumbs.Profile.StudentsIdentifier): _*)
	}



}
