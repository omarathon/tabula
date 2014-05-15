package uk.ac.warwick.tabula.attendance.commands.manage

import uk.ac.warwick.tabula.data.model.StudentMember

abstract class SchemeMembershipItemType(val value: String)
case object StaticType extends SchemeMembershipItemType("static")
case object IncludeType extends SchemeMembershipItemType("include")
case object ExcludeType extends SchemeMembershipItemType("exclude")

/**
 * Item in list of members for displaying in view.
 */
case class SchemeMembershipItem(
	member: StudentMember,
	itemType: SchemeMembershipItemType // static, include or exclude
) {
	def itemTypeString = itemType.value
}
