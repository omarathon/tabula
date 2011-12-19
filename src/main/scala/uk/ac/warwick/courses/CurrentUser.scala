package uk.ac.warwick.courses
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.AnonymousUser

/**
 * Represents... surprise, the current user.
 * 
 * Like SitebuilderUser in Sitebuilder, we want to add certain
 * things on top of the regular SSO user, and even store more than
 * one representation of a user (when masquerading).
 */
class CurrentUser(
		val realUser:User, 
		val apparentUser:User, 
		val sysadmin:Boolean=false, 
		val masquerader:Boolean=false, 
		val god:Boolean=false) {
	
	def loggedIn = realUser.isLoggedIn
	def idForPermissions = apparentUser.getUserId()
	
	def apparentId = apparentUser.getUserId
	def realId = realUser.getUserId
	def masquerading = !apparentId.equals(realId)
	
	def fullName = apparentUser.getFullName
	def firstName = apparentUser.getFirstName
	def universityId = apparentUser.getWarwickId
	
	override def toString = {
      val builder = new StringBuilder("User ")
      builder append idForPermissions
      if (masquerading) {
    	  builder append " (really " 
    	  builder append realUser.getUserId
    	  builder append ")"
      }
      if (god) builder append " +GodMode"
      builder.toString
    }
}

object CurrentUser {
	val keyName = "CurrentUser"
}

object NoCurrentUser {
	def apply() = {
		val anon = new AnonymousUser
		new CurrentUser(realUser=anon, apparentUser=anon)
	}
}
