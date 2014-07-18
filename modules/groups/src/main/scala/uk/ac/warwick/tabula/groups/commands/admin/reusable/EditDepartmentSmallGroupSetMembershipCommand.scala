package uk.ac.warwick.tabula.groups.commands.admin.reusable

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.UniversityId
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.LazyLists

case class EditDepartmentSmallGroupSetMembershipCommandResult(
	updatedIncludedStudentIds: JList[String],
	updatedExcludedStudentIds: JList[String],
	membershipItems: Seq[DepartmentSmallGroupSetMembershipItem]
)

case class AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(
	missingUsers: Seq[String]
)

object EditDepartmentSmallGroupSetMembershipCommand {
	def apply(set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupSetMembershipCommandInternal(set)
			with AutowiringUserLookupComponent
			with ComposableCommand[EditDepartmentSmallGroupSetMembershipCommandResult]
			with PopulateEditDepartmentSmallGroupSetMembershipCommand
			with AddsUsersToEditDepartmentSmallGroupSetMembershipCommand
			with RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand
			with ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand
			with EditDepartmentSmallGroupSetMembershipPermissions
			with EditDepartmentSmallGroupSetMembershipCommandState
			with Unaudited with ReadOnly
}

/**
 * Not persisted, just used to validate users entered and render student table
 */
class EditDepartmentSmallGroupSetMembershipCommandInternal(val set: DepartmentSmallGroupSet)
	extends CommandInternal[EditDepartmentSmallGroupSetMembershipCommandResult] {

	self: EditDepartmentSmallGroupSetMembershipCommandState with UserLookupComponent =>

	override def applyInternal() = {
		def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
			val user = userLookup.getUserByWarwickUniId(universityId)
			DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
		}

		val membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
			val excludedMemberItems = updatedExcludedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipExcludeType))
			val includedMemberItems = updatedIncludedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipIncludeType))
			(excludedMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
		}

		EditDepartmentSmallGroupSetMembershipCommandResult(
			updatedIncludedStudentIds,
			updatedExcludedStudentIds,
			membershipItems
		)
	}

}

trait PopulateEditDepartmentSmallGroupSetMembershipCommand extends PopulateOnForm {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	override def populate() = {
		updatedIncludedStudentIds = includedStudentIds
		updatedExcludedStudentIds = excludedStudentIds
	}

}

trait AddsUsersToEditDepartmentSmallGroupSetMembershipCommand {
	self: EditDepartmentSmallGroupSetMembershipCommandState with UserLookupComponent =>

	def addUsers(): AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult = {
		def getUserForString(entry: String): Option[User] = {
			if (UniversityId.isValid(entry)) {
				val user = userLookup.getUserByWarwickUniId(entry)

				if (user.isFoundUser) Some(user)
				else None
			} else {
				val user = userLookup.getUserByUserId(entry)

				if (user.isFoundUser) Some(user)
				else None
			}
		}

		val massAddedUserMap: Map[String, Option[User]] = massAddUsersEntries.map{ entry =>
			entry -> getUserForString(entry)
		}.toMap

		val missingUsers = massAddedUserMap.filter(!_._2.isDefined).keys.toSeq
		val validUsers = massAddedUserMap.filter(_._2.isDefined).values.flatten.toSeq

		updatedIncludedStudentIds = (updatedIncludedStudentIds.asScala.toSeq ++ validUsers.map(_.getWarwickId)).asJava
		updatedExcludedStudentIds = (updatedExcludedStudentIds.asScala.toSeq diff updatedIncludedStudentIds.asScala.toSeq).asJava

		// Users processed, so reset fields
		massAddUsers = ""

		AddUsersToEditDepartmentSmallGroupSetMembershipCommandResult(missingUsers)
	}

}

trait RemovesUsersFromEditDepartmentSmallGroupSetMembershipCommand {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	def removeUsers() = {
		updatedExcludedStudentIds = (updatedExcludedStudentIds.asScala ++ excludeIds.asScala).distinct.asJava
	}
}

trait ResetsMembershipInEditDepartmentSmallGroupSetMembershipCommand {

	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	def resetMembership() = {
		updatedIncludedStudentIds = (updatedIncludedStudentIds.asScala diff resetStudentIds.asScala).asJava
		updatedExcludedStudentIds = (updatedExcludedStudentIds.asScala diff resetStudentIds.asScala).asJava
	}

	def resetAllIncluded() = {
		updatedIncludedStudentIds.clear()
	}

	def resetAllExcluded() = {
		updatedExcludedStudentIds.clear()
	}
}


trait EditDepartmentSmallGroupSetMembershipPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupSetMembershipCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}

}

trait EditDepartmentSmallGroupSetMembershipCommandState {
	def set: DepartmentSmallGroupSet

	// Bind variables

	// Store original students for reset
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()

	// Store updated students
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()

	var massAddUsers: String = _
	// parse massAddUsers into a collection of individual tokens
	def massAddUsersEntries: Seq[String] =
		if (massAddUsers == null) Nil
		else massAddUsers split "(\\s|[^A-Za-z\\d\\-_\\.])+" map (_.trim) filterNot (_.isEmpty)

	var excludeIds: JList[String] = LazyLists.create()
	var resetStudentIds: JList[String] = LazyLists.create()
}
