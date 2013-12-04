package uk.ac.warwick.tabula.data.convert
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.system.TwoWayConverter
import uk.ac.warwick.userlookup.User

/**
 *
 * TODO - Work out a better way of doing this!
 *
 * We need to convert users from both usercodes and ids. Convert right will attempt to convert as if the supplied
 * string was a usercode. If this fails then we will try to convert based on Warwick Id instead.
 *
 * Convert left will always transform a User to a usercode.
 */
class UserConverter extends TwoWayConverter[String, User] {
	
	var userLookup = Wire.auto[UserLookupService]

	override def convertRight(userId: String) = {
		val userFromCode = userLookup.getUserByUserId(userId)
		if (userFromCode == null)
			userLookup.getUserByWarwickUniId(userId)
		else
			userFromCode
	}
	override def convertLeft(user: User) = (Option(user) map { _.getUserId }).orNull 

}