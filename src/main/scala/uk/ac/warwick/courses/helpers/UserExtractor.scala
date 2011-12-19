package uk.ac.warwick.courses.helpers
import uk.ac.warwick.userlookup.User

/**
 * A pattern which you can use in a match statement to match a User
 * object that exists and isn't an anonymous user.
 */
object FoundUser {
	def unapply(user:User): Option[User] = if (user.isFoundUser()) Some(user) else None
}

object NoUser {
	def unapply(user:User): Option[User] = if (user.isFoundUser()) None else Some(user)
}