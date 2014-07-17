package uk.ac.warwick.tabula.groups.commands.admin.reusable

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.groups.DepartmentSmallGroupSet
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent, AutowiringUserLookupComponent, UserLookupComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import scala.collection.JavaConverters._

object UpdateStudentsForDepartmentSmallGroupSetCommand {
	def apply(set: DepartmentSmallGroupSet) =
		new UpdateStudentsForDepartmentSmallGroupSetCommandInternal(set)
			with ComposableCommand[DepartmentSmallGroupSet]
			with UpdateStudentsForDepartmentSmallGroupSetPermissions
			with UpdateStudentsForDepartmentSmallGroupSetDescription
			with PopulateUpdateStudentsForDepartmentSmallGroupSetCommandInternal
			with SetStudents
			with AutowiringUserLookupComponent
			with AutowiringSmallGroupServiceComponent
}

class UpdateStudentsForDepartmentSmallGroupSetCommandInternal(val set: DepartmentSmallGroupSet)
	extends CommandInternal[DepartmentSmallGroupSet] with UpdateStudentsForDepartmentSmallGroupSetCommandState {
	self: UserLookupComponent with SmallGroupServiceComponent =>

	override def applyInternal() = {
		set.members.knownType.staticUserIds = staticStudentIds.asScala
		set.members.knownType.includedUserIds = includedStudentIds.asScala
		set.members.knownType.excludedUserIds = excludedStudentIds.asScala
		set.memberQuery = filterQueryString
		smallGroupService.saveOrUpdate(set)

		set
	}
}

trait PopulateUpdateStudentsForDepartmentSmallGroupSetCommandInternal extends PopulateOnForm {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	override def populate() = {
		staticStudentIds = set.members.knownType.staticUserIds.asJava
		includedStudentIds = set.members.knownType.includedUserIds.asJava
		excludedStudentIds = set.members.knownType.excludedUserIds.asJava
		filterQueryString = set.memberQuery
	}
}

trait UpdateStudentsForDepartmentSmallGroupSetCommandState {
	self: UserLookupComponent =>

	def set: DepartmentSmallGroupSet

	def membershipItems: Seq[DepartmentSmallGroupSetMembershipItem] = {
		def toMembershipItem(universityId: String, itemType: DepartmentSmallGroupSetMembershipItemType) = {
			val user = userLookup.getUserByWarwickUniId(universityId)
			DepartmentSmallGroupSetMembershipItem(itemType, user.getFirstName, user.getLastName, user.getWarwickId, user.getUserId)
		}

		val staticMemberItems =
			((staticStudentIds.asScala diff excludedStudentIds.asScala) diff includedStudentIds.asScala)
				.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipStaticType))

		val includedMemberItems = includedStudentIds.asScala.map(toMembershipItem(_, DepartmentSmallGroupSetMembershipIncludeType))

		(staticMemberItems ++ includedMemberItems).sortBy(membershipItem => (membershipItem.lastName, membershipItem.firstName))
	}

	// Bind variables

	// Students to persist
	var includedStudentIds: JList[String] = LazyLists.create()
	var excludedStudentIds: JList[String] = LazyLists.create()
	var staticStudentIds: JList[String] = LazyLists.create()
	var filterQueryString: String = ""

	// Students from Select page
	var updatedIncludedStudentIds: JList[String] = LazyLists.create()
	var updatedExcludedStudentIds: JList[String] = LazyLists.create()
	var updatedStaticStudentIds: JList[String] = LazyLists.create()
	var updatedFilterQueryString: String = ""
}

trait SetStudents {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	def linkToSits() = {
		includedStudentIds.clear()
		includedStudentIds.addAll(updatedIncludedStudentIds)
		excludedStudentIds.clear()
		excludedStudentIds.addAll(updatedExcludedStudentIds)
		staticStudentIds.clear()
		staticStudentIds.addAll(updatedStaticStudentIds)
		filterQueryString = updatedFilterQueryString
	}

	def importAsList() = {
		includedStudentIds.clear()
		excludedStudentIds.clear()
		staticStudentIds.clear()
		val newList = ((updatedStaticStudentIds.asScala diff updatedExcludedStudentIds.asScala) ++ updatedIncludedStudentIds.asScala).distinct
		includedStudentIds.addAll(newList.asJava)
		filterQueryString = ""
	}
}

trait UpdateStudentsForDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Update, set)
	}

}

trait UpdateStudentsForDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: UpdateStudentsForDepartmentSmallGroupSetCommandState =>

	override lazy val eventName = "UpdateStudentsForDepartmentSmallGroupSet"

	override def describe(d: Description) {
		d.properties("smallGroupSet" -> set.id)
	}
}
