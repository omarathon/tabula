package uk.ac.warwick.tabula.data.model

/**
  * Item in list of members for displaying in view.
  */
case class UserGroupMembershipItem(
  itemType: UserGroupMembershipType,
  firstName: String,
  lastName: String,
  universityId: String,
  userId: String
) {
  def itemTypeString: String = itemType.value
}


sealed abstract class UserGroupMembershipType(val value: String)

object UserGroupMembershipType {

  case object Static extends UserGroupMembershipType("static")

  case object Include extends UserGroupMembershipType("include")

  case object Exclude extends UserGroupMembershipType("exclude")

}
