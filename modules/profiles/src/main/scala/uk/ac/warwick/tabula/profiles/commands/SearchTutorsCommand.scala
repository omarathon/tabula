package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.model.MemberUserType
import uk.ac.warwick.tabula.data.model.Staff

class SearchTutorsCommand(user: CurrentUser) extends AbstractSearchProfilesCommand(user, Staff) {
	var deptDao = Wire.auto[DepartmentDao]

	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()
		
	private def queryMatches = {
		profileService.findMembersByQuery(query, deptDao.allDepartments, userTypes, user.god)
	}
}
