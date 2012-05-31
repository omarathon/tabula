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
	def exists = realUser.isFoundUser
	
	/** The user who we are acting as. This is the actual user when not masquerading,
	  * otherwise it's whoever you're pretending to be.
	  */
	def apparentId = apparentUser.getUserId
	/** This is always the user ID of the actual person signed in. Normally only for
	  * use by the audit logging framework.
	  */
	def realId = realUser.getUserId
	/** Whether you're currently masquerading as someone else. */
	def masquerading = !apparentId.equals(realId)
	
	/** Full name of the apparent user. */
	def fullName = apparentUser.getFullName
	/** First name of the apparent user. */
	def firstName = apparentUser.getFirstName
	/** Warwick Uni ID of the apparent user. */
	def universityId = apparentUser.getWarwickId
	/** Department name of the apparent user. */
	def departmentName = apparentUser.getDepartment
	/** Email address of the apparent user. */
	def email = apparentUser.getEmail
	
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
