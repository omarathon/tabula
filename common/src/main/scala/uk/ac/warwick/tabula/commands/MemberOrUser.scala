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
  def apply(member: Member): MemberOrUser = WrappedMember(member)

  def apply(user: User): MemberOrUser = WrappedUser(user)

  def apply(member: Option[Member], user: User): MemberOrUser = member map {
    WrappedMember
  } getOrElse WrappedUser(user)
}

sealed trait MemberOrUser {
  def isMember: Boolean

  def isStaff: Boolean

  def fullName: Option[String]

  def firstName: String

  def lastName: String

  def universityId: String

  def usercode: String

  def shortDepartment: String

  def departmentCode: String

  def email: String

  def asUser: User

  def asMember: Option[Member]

  override def hashCode: Int = universityId.hashCode

  override def equals(other: Any): Boolean = other match {
    case other: MemberOrUser => other.universityId == universityId
    case _ => false
  }
}

private case class WrappedUser(user: User) extends MemberOrUser {
  override val isMember = false

  override def isStaff: Boolean = user.isStaff

  override def fullName: Option[String] = Some(user.getFullName)

  override def firstName: String = user.getFirstName

  override def lastName: String = user.getLastName

  override def universityId: String = user.getWarwickId

  override def usercode: String = user.getUserId

  override def shortDepartment: String = user.getShortDepartment

  override def departmentCode: String = user.getDepartmentCode

  override def email: String = user.getEmail

  override val asUser: User = user

  override val asMember: Option[Member] = None
}

private case class WrappedMember(member: Member) extends MemberOrUser {
  override val isMember = true

  override def isStaff: Boolean = member.isStaff

  override def fullName: Option[String] = member.fullName

  override def firstName: String = member.firstName

  override def lastName: String = member.lastName

  override def universityId: String = member.universityId

  override def usercode: String = member.userId

  override def shortDepartment: String = member.homeDepartment.name

  override def departmentCode: String = member.homeDepartment.code

  override def email: String = member.email

  override def asUser: User = member.asSsoUser

  override val asMember: Option[Member] = Some(member)
}
