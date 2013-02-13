package uk.ac.warwick.tabula.profiles.commands

import scala.reflect.BeanProperty

import org.hibernate.validator.constraints.NotEmpty

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.Command
import uk.ac.warwick.tabula.commands.Description
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Student
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.services.ProfileService
import uk.ac.warwick.tabula.services.SecurityService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.tabula.data.model.Student

class SearchProfilesCommand(val currentMember: Member, user: CurrentUser) extends AbstractSearchProfilesCommand(user, Student) {

	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()
		
	private def queryMatches = {
		val depts = (currentMember.affiliatedDepartments ++ moduleService.departmentsOwnedBy(currentMember.userId)).distinct
		profileService.findMembersByQuery(query, depts, userTypes, user.god)
	}
}
