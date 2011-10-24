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
class CurrentUser(val realUser:User, val sysadmin:Boolean, val pretendUser:User) {
    var sysadminEnabled = false

    def this(realUser:User, sysadmin:Boolean) = this(realUser, sysadmin, realUser)
  
	def loggedIn = realUser.isLoggedIn
}

object CurrentUser {
  val keyName = "CurrentUser"
}

object NoCurrentUser {
	def apply = new CurrentUser(new AnonymousUser, false)
}
