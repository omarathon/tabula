package uk.ac.warwick.tabula.commands

import uk.ac.warwick.tabula.data.model.Member
import uk.ac.warwick.userlookup.User

/**
 * Wrapper that exposes properties from Member if available, with User
 * as a fallback. Useful where we get some User objects from a UserGroup
 * but want to look up profiles as preferred info where available.
 *
 * Currently only used in one place but feel free to expand this out to
 * something more generally usable.
 *
 * TODO maybe Member could directly implement the trait
 */
object MemberOrUser {
	def apply(member: Option[Member], user: User) = member map { WrappedMember(_) } getOrElse WrappedUser(user)
}

sealed trait MemberOrUser{
	def isMember: Boolean
	def fullName: Option[String]
	def firstName: String
	def lastName: String
	def universityId: String
	def shortDepartment: String
	def email: String
	
	override def hashCode = universityId.hashCode
	
	override def equals(other: Any) = other match {
		case other: MemberOrUser => other.universityId == universityId
		case _ => false
	}
}

private case class WrappedUser(user: User) extends MemberOrUser {
	def isMember = false
	def fullName = Some(user.getFullName)
	def firstName = user.getFirstName
	def lastName = user.getLastName
	def universityId = user.getWarwickId
	def shortDepartment = user.getShortDepartment
	def email = user.getEmail
}

private case class WrappedMember(member: Member) extends MemberOrUser {
	def isMember = true
	def fullName = member.fullName
	def firstName = member.firstName
	def lastName = member.lastName
	def universityId = member.universityId
	def shortDepartment = member.homeDepartment.name
	def email = member.email
}