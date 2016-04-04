package uk.ac.warwick.tabula.commands.profiles

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.model.MemberUserType.Staff
import uk.ac.warwick.tabula.services.elasticsearch.ProfileQueryService

class SearchAgentsCommand(user: CurrentUser) extends AbstractSearchProfilesCommand(user, Staff) {
	var deptDao = Wire.auto[DepartmentDao]
	var profileQueryService = Wire[ProfileQueryService]

	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()

	private def queryMatches = {
		profileQueryService.findWithQuery(query, Seq(), includeTouched = false, userTypes, searchAcrossAllDepartments = true)
	}
}
