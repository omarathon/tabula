package uk.ac.warwick.tabula.api.web.controllers.profiles

import org.hibernate.criterion.Restrictions
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.api.web.controllers.ApiController
import uk.ac.warwick.tabula.api.web.helpers.MemberApiFreemarkerHelper
import uk.ac.warwick.tabula.commands.ViewViewableCommand
import uk.ac.warwick.tabula.data.ScalaRestriction
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsTarget}
import uk.ac.warwick.tabula.services.{AutowiringProfileServiceComponent, ProfileServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.views.JSONView

@Controller
@RequestMapping(Array("/v1/membersearch"))
class MemberSearchController extends ApiController
	with GetMembersApi with AutowiringProfileServiceComponent {

	final override def onPreRequest {
		session.enableFilter(Member.ActiveOnlyFilter)
	}

	@RequestMapping(method = Array(GET), produces = Array("application/json"))
	def search(
		@ModelAttribute("getCommand") command: ViewViewableCommand[PermissionsTarget],
		@RequestParam query: String
	): Mav = {
		if (query.safeTrim.length < 3) {
			getMav(Seq())
		} else {
			if (UniversityId.isValid(query.safeTrim)) {
				getMav(profileService.getMemberByUniversityId(query.safeTrim, disableFilter = true).toSeq)
			} else {
				val usercodeMembers = profileService.getAllMembersWithUserId(query.safeTrim, disableFilter = true)
				if (usercodeMembers.nonEmpty) {
					getMav(usercodeMembers)
				} else if (query.contains("@")) {
					val usercodes = profileService.findAllUserIdsByRestrictions(Seq(new ScalaRestriction(
						Restrictions.disjunction()
							.add(Restrictions.eq("email", query.safeTrim).ignoreCase())
							.add(Restrictions.eq("homeEmail", query.safeTrim).ignoreCase())
					)))
					getMav(usercodes.flatMap(usercode => profileService.getAllMembersWithUserId(usercode, disableFilter = true)))
				} else {
					getMav(profileService.findMembersByQuery(query.safeTrim, departments = Seq(), userTypes = Set(), searchAllDepts = true, activeOnly = true))
				}
			}
		}
	}
}


trait GetMembersApi {

	self: ApiController with ProfileServiceComponent =>

	@ModelAttribute("getCommand")
	def getCommand(): ViewViewableCommand[PermissionsTarget] =
		new ViewViewableCommand(Permissions.Profiles.ViewSearchResults, PermissionsTarget.Global)

	def getMav(members: Seq[Member]): Mav = {
		Mav(new JSONView(Map(
			"success" -> true,
			"status" -> "ok",
			"results" -> members.map(member => Map(
				"universityId" -> member.universityId,
				"userId" -> member.userId,
				"firstName" -> member.firstName,
				"lastName" -> member.lastName,
				"email" -> member.email,
				"homeEmail" -> member.homeEmail,
				"department" -> MemberApiFreemarkerHelper.departmentToJson(member.homeDepartment),
				"userType" -> member.userType.description
			))
		)))
	}
}
