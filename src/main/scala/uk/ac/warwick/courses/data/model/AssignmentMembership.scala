package uk.ac.warwick.courses.data.model

import uk.ac.warwick.courses.services.UserLookupService
import uk.ac.warwick.userlookup.User
import collection.JavaConversions._
import uk.ac.warwick.courses.helpers.FoundUser

object AssignmentMembership {

	/** Determine a unified list of membership for an assignment based on a SITS group,
	  * a list of manual users to include and a list of manual users to exclude.
	  * Note that the size of the returned collection is NOT the number of members in the
	  * list, since we list users who we're excluding. 
	  *
	  * The SITS group is optional.
	  * 
	  * This would be a method on Assignment if it weren't for the fact that we need
	  * to be able to calculate this elsewhere such as on the create new assignment command.
	  */
	def determineMembership(upstream: Option[UpstreamAssessmentGroup], others: UserGroup)(userLookup: UserLookupService): Seq[MembershipItem] = {

		val sitsUsers = upstream map { upstream =>
			upstream.members.members map { id =>
			    id -> userLookup.getUserByWarwickUniId(id)
		    }
		} getOrElse Nil
		val includes = others.includeUsers map { id => id -> userLookup.getUserByUserId(id) }
		val excludes = others.excludeUsers map { id => id -> userLookup.getUserByUserId(id) }

		// convert lists of Users to lists of MembershipItems that we can render neatly together.

		val includeItems = makeIncludeItems(includes, sitsUsers)
		val excludeItems = makeExcludeItems(excludes, sitsUsers)
		val sitsItems = makeSitsItems(includes, excludes, sitsUsers)

		includeItems ++ excludeItems ++ sitsItems
	}

	private def sameUserIdAs(user: User) = (other: Pair[String,User]) => { user.getUserId == other._2.getUserId }
	private def in(seq: Seq[Pair[String,User]]) = (other: Pair[String,User]) => { seq exists sameUserIdAs(other._2) }

	private def makeIncludeItems(includes: Seq[Pair[String,User]], sitsUsers: Seq[Pair[String,User]]) = 
		includes map { case (id, user) =>
			val extraneous = sitsUsers exists sameUserIdAs(user)
			MembershipItem(
				user = user,
				universityId = universityId(user, None),
                userId = userId(user, Some(id)),
				itemType = "include",
				extraneous = extraneous)
		}
	

	private def makeExcludeItems(excludes: Seq[Pair[String,User]], sitsUsers: Seq[Pair[String,User]]) = 
		excludes map { case (id, user) =>
			val extraneous = !(sitsUsers exists sameUserIdAs(user))
			MembershipItem(
				user = user,
                universityId = universityId(user, None),
                userId = userId(user, Some(id)),
                itemType = "exclude",
				extraneous = extraneous)
		}
	

	private def makeSitsItems(includes: Seq[Pair[String,User]], excludes: Seq[Pair[String,User]], sitsUsers: Seq[Pair[String,User]]) = 
		sitsUsers filterNot in(includes) filterNot in(excludes) map { case (id,user) =>
			MembershipItem(
				user = user,
                universityId = universityId(user, Some(id)),
                userId = userId(user, None),
				itemType = "sits",
				extraneous = false)
		}
	
	private def universityId(user:User, fallback:Option[String]) = option(user) map { _.getWarwickId } orElse fallback
	private def userId(user:User, fallback:Option[String]) = option(user) map { _.getUserId } orElse fallback

	private def option(user:User): Option[User] = user match {
		case FoundUser(u) => Some(user)
		case _ => None
	}
}





/** Item in list of members for displaying in view. */
case class MembershipItem(
    user: User,
    universityId: Option[String],
    userId: Option[String],
    itemType: String, // sits, include or exclude
    /**
     * If include type, this item adds a user who's already in SITS.
     * If exclude type, this item excludes a user who isn't in the list anyway.
     */
    extraneous: Boolean 
)

