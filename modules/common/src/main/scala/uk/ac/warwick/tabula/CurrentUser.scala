package uk.ac.warwick.tabula

import uk.ac.warwick.tabula.system.UserNavigation
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.userlookup.AnonymousUser
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.data.model.Member

/**
 * Represents... surprise, the current user.
 *
 * Like SitebuilderUser in Sitebuilder, we want to add certain
 * things on top of the regular SSO user, and even store more than
 * one representation of a user (when masquerading).
 */
class CurrentUser(
	val realUser: User,
	/**
	 * This is the User that should be referenced almost all of the time. It is either
	 * the actual user, or the person you are masquerading as.
	 */
	val apparentUser: User,
	val profile: Option[Member] = None,
	val sysadmin: Boolean = false,
	val masquerader: Boolean = false,
	val god: Boolean = false,
	var navigation: UserNavigation = UserNavigation("", "")
) {

	def loggedIn = realUser.isLoggedIn
	def idForPermissions = apparentUser.getUserId
	def exists = realUser.isFoundUser

	/**
	 * The user who we are acting as. This is the actual user when not masquerading,
	 * otherwise it's whoever you're pretending to be.
	 */
	def apparentId = apparentUser.getUserId
	/**
	 * This is always the user ID of the actual person signed in. Normally only for
	 * use by the audit logging framework.
	 */
	def realId = realUser.getUserId
	/** Whether you're currently masquerading as someone else. */
	def masquerading = !apparentId.equals(realId)

	/** Full name of the apparent user. */
	def fullName = profile flatMap { _.fullName } getOrElse apparentUser.getFullName
	/** First name of the apparent user. */
	def firstName = profile map { _.firstName } getOrElse apparentUser.getFirstName
	/** Surname of the apparent user. */
	def lastName = profile map { _.lastName } getOrElse apparentUser.getLastName
	/** Warwick Uni ID of the apparent user. */
	def universityId = apparentUser.getWarwickId
	/** Department code of the apparent user. */
	def departmentCode = apparentUser.getDepartmentCode
	/** Department name of the apparent user. */
	def departmentName = apparentUser.getDepartment
	/** Email address of the apparent user. */
	def email = apparentUser.getEmail
	/** User code of the apparent user. */
	def userId = apparentUser.getUserId

	/** Is of type Postgraduate research student (FT )? (includes PT) */
	def isPGR = apparentUser.getExtraProperty("warwickitsclass") == "PG(R)"

	/** Is of type Student? (includes PGT) */
	def isStudent = apparentUser.isStudent

	/** Is of type Staff? (includes PGR) */
	def isStaff = apparentUser.isStaff

	def isAlumni = apparentUser.isAlumni

	def isMember = isStudent || isStaff

	override def toString = {
		if (!idForPermissions.hasText) "Anonymous user"
		else {
			val builder = new StringBuilder("User ")
			builder append idForPermissions
			if (masquerading) {
				builder append " (really "
				builder append realUser.getUserId
				builder append ")"
			}
			if (god) builder append " +GodMode"
			builder.toString()
		}
	}

	override def equals(that: Any): Boolean = that match {
		case other: CurrentUser => other.apparentUser == this.apparentUser && other.realUser == this.realUser
		case _ => super.equals(that)
	}
}

object CurrentUser {
	val keyName = "CurrentUser"
	val masqueradeCookie = "tabulaMasqueradeAs"
	val godModeCookie = "tabulaGodMode"
}

object NoCurrentUser {
	def apply() = {
		val anon = new AnonymousUser
		new CurrentUser(realUser = anon, apparentUser = anon)
	}
}
