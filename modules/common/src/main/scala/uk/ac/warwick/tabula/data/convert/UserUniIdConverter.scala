package uk.ac.warwick.tabula.data.convert

import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService

class UserUniIdConverter extends TwoWayConverter[String, User] {

	var userLookup = Wire.auto[UserLookupService]

	override def convertRight(uniId: String) = userLookup.getUserByWarwickUniId(uniId)
	override def convertLeft(user: User) = (Option(user) map { _.getWarwickId }).orNull

}
