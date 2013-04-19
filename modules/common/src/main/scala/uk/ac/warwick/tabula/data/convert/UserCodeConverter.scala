package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.helpers.Promises._

class UserCodeConverter extends TwoWayConverter[String, User] {
	
	var userLookup = promise { Wire[UserLookupService] }
  	
	override def convertRight(userId: String) = userLookup.get.getUserByUserId(userId)
	override def convertLeft(user: User) = (Option(user) map { _.getUserId }).orNull 

}