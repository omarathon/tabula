package uk.ac.warwick.tabula.profiles.commands

import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.data.DepartmentDao
import uk.ac.warwick.tabula.data.model.Staff
import uk.ac.warwick.tabula.services.ProfileIndexService

class SearchTutorsCommand(user: CurrentUser) extends AbstractSearchProfilesCommand(user, Staff) {
	var deptDao = Wire.auto[DepartmentDao]
	var profileIndexService = Wire.auto[ProfileIndexService]
	
	override def applyInternal() =
		if (validQuery) usercodeMatches ++ universityIdMatches ++ queryMatches
		else Seq()
		
	private def queryMatches = {
		profileIndexService.findWithQuery(query, Seq(), userTypes, true)
	}
}
