package uk.ac.warwick.tabula.helpers
import uk.ac.warwick.userlookup.User

import scala.language.implicitConversions

object UserOrdering {

	implicit def orderedUsers(u: User): math.Ordered[User] = new math.Ordered[User] {

		import scala.math.Ordered.orderingToOrdered

		override def compare(u2: User): Int = {
			(u.getLastName, u.getFirstName) compare (u2.getFirstName, u2.getLastName)
		}
	}
}
