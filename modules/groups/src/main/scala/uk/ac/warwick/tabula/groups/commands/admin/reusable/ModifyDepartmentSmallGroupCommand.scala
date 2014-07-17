package uk.ac.warwick.tabula.groups.commands.admin.reusable

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model.groups.{DepartmentSmallGroup, DepartmentSmallGroupSet, SmallGroup}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object ModifyDepartmentSmallGroupCommand {
	def create(set: DepartmentSmallGroupSet) =
		new CreateDepartmentSmallGroupCommandInternal(set)
			with ComposableCommand[DepartmentSmallGroup]
			with ModifyDepartmentSmallGroupCommandValidation
			with CreateDepartmentSmallGroupPermissions
			with CreateDepartmentSmallGroupDescription
			with AutowiringSmallGroupServiceComponent

	def edit(group: DepartmentSmallGroup) =
		new EditDepartmentSmallGroupCommandInternal(group)
			with ComposableCommand[DepartmentSmallGroup]
			with ModifyDepartmentSmallGroupCommandValidation
			with EditDepartmentSmallGroupPermissions
			with EditDepartmentSmallGroupDescription
			with AutowiringSmallGroupServiceComponent
}

trait ModifyDepartmentSmallGroupState {
	def smallGroupSet: DepartmentSmallGroupSet

	var name: String = _

	var maxGroupSize: Int = SmallGroup.DefaultGroupSize

	// Used by parent command
	var delete: Boolean = false
}

trait CreateDepartmentSmallGroupCommandState extends ModifyDepartmentSmallGroupState

class CreateDepartmentSmallGroupCommandInternal(val smallGroupSet: DepartmentSmallGroupSet) extends ModifyDepartmentSmallGroupCommandInternal with CreateDepartmentSmallGroupCommandState {
	self: SmallGroupServiceComponent =>

	def applyInternal() = transactional() {
		val group = new DepartmentSmallGroup
		copyTo(group)

		// FIXME This is to avoid the un-saved transient Hibernate bug
		if (smallGroupSet.id.hasText) smallGroupService.saveOrUpdate(group)

		group
	}
}

trait EditDepartmentSmallGroupCommandState extends ModifyDepartmentSmallGroupState {
	def smallGroup: DepartmentSmallGroup
	lazy val smallGroupSet = smallGroup.groupSet
}

class EditDepartmentSmallGroupCommandInternal(val smallGroup: DepartmentSmallGroup) extends ModifyDepartmentSmallGroupCommandInternal with EditDepartmentSmallGroupCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(smallGroup)

	def applyInternal() = transactional() {
		copyTo(smallGroup)
		smallGroup
	}
}

abstract class ModifyDepartmentSmallGroupCommandInternal
	extends CommandInternal[DepartmentSmallGroup] with ModifyDepartmentSmallGroupState {

	def copyFrom(group: DepartmentSmallGroup) {
		name = group.name
		group.maxGroupSize.foreach(size => maxGroupSize = size)
	}

	def copyTo(group: DepartmentSmallGroup) {
		group.name = name
		group.maxGroupSize = maxGroupSize
	}
}

trait ModifyDepartmentSmallGroupCommandValidation extends SelfValidating {
	self: ModifyDepartmentSmallGroupState =>

	override def validate(errors: Errors) {
		// Skip validation when this group is being deleted
		if (!delete) {
			if (!name.hasText) errors.rejectValue("name", "smallGroup.name.NotEmpty")
			else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroup.name.Length", Array[Object](200: JInteger), "")
		}
	}
}

trait CreateDepartmentSmallGroupPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateDepartmentSmallGroupCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(smallGroupSet))
	}
}

trait CreateDepartmentSmallGroupDescription extends Describable[DepartmentSmallGroup] {
	self: CreateDepartmentSmallGroupCommandState =>

	override def describe(d: Description) {
		d.department(smallGroupSet.department).properties("smallGroupSet" -> smallGroupSet.id, "name" -> name)
	}

	override def describeResult(d: Description, group: DepartmentSmallGroup) =
		d.department(group.groupSet.department).properties("smallGroupSet" -> group.groupSet.id, "smallGroup" -> group.id)
}

trait EditDepartmentSmallGroupPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(smallGroup))
	}
}

trait EditDepartmentSmallGroupDescription extends Describable[DepartmentSmallGroup] {
	self: EditDepartmentSmallGroupCommandState =>

	override def describe(d: Description) {
		d.department(smallGroupSet.department).properties("smallGroupSet" -> smallGroupSet.id, "smallGroup" -> smallGroup.id)
	}
}