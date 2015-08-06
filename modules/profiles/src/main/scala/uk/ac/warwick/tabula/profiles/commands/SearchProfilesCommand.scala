package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.{Department, Member}
import uk.ac.warwick.tabula.data.model.MemberUserType.Student
import uk.ac.warwick.tabula.permissions.Permissions
import collection.JavaConverters._

class SearchProfilesCommand(val currentMember: Member, user: CurrentUser) extends AbstractSearchProfilesCommand(user, Student) {

	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()

	private def queryMatches = {
		val depts = (currentMember.affiliatedDepartments ++ moduleService.departmentsWithPermission(user, Permissions.Profiles.ViewSearchResults)).distinct
		val deptsAndDescendantDepts = depts.flatMap(dept => {
			def children(d: Department): Set[Department] = {
				if (!d.hasChildren) Set(d)
				else Set(d) ++ d.children.asScala.toSet.flatMap(children)
			}
			Set(dept) ++ children(dept)
		})
		profileService.findMembersByQuery(query, deptsAndDescendantDepts, userTypes, user.god)
	}
}
