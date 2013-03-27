package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User

class UserCodeConverter extends TwoWayConverter[String, User] {
	
	var userLookup = Wire.auto[UserLookupService]
  	
	override def convertRight(userId: String) = userLookup.getUserByUserId(userId)
	override def convertLeft(user: User) = (Option(user) map { _.getUserId }).orNull 

}