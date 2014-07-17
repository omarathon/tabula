package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.helpers.LazyLists
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringSmallGroupServiceComponent, SmallGroupServiceComponent}
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User
import scala.collection.JavaConverters._
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._

object ModifyDepartmentSmallGroupSetCommand {
	def create(department: Department) =
		new CreateDepartmentSmallGroupSetCommandInternal(department)
			with ComposableCommand[DepartmentSmallGroupSet]
			with ModifyDepartmentSmallGroupSetCommandValidation
			with CreateDepartmentSmallGroupSetPermissions
			with CreateDepartmentSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent

	def edit(set: DepartmentSmallGroupSet) =
		new EditDepartmentSmallGroupSetCommandInternal(set)
			with ComposableCommand[DepartmentSmallGroupSet]
			with ModifyDepartmentSmallGroupSetCommandValidation
			with EditDepartmentSmallGroupSetPermissions
			with EditDepartmentSmallGroupSetDescription
			with AutowiringSmallGroupServiceComponent
}

trait ModifyDepartmentSmallGroupSetState extends CurrentAcademicYear {
	def existingSet: Option[DepartmentSmallGroupSet]

	var name: String = _

	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	var allowSelfGroupSwitching: Boolean = true
	var defaultMaxGroupSizeEnabled: Boolean = false
	var defaultMaxGroupSize: Int = SmallGroup.DefaultGroupSize

	var groups: JList[Appliable[DepartmentSmallGroup] with ModifyDepartmentSmallGroupState with SelfValidating] = LazyLists.create { () =>
		ModifyDepartmentSmallGroupCommand.create(existingSet)
	}
}

trait CreateDepartmentSmallGroupSetCommandState extends ModifyDepartmentSmallGroupSetState {
	def department: Department
	val existingSet = None
}

class CreateDepartmentSmallGroupSetCommandInternal(val department: Department) extends ModifyDepartmentSmallGroupSetCommandInternal with CreateDepartmentSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	def applyInternal() = transactional() {
		val set = new DepartmentSmallGroupSet(department)
		copyTo(set)
		smallGroupService.saveOrUpdate(set)
		set
	}

}

trait EditDepartmentSmallGroupSetCommandState extends ModifyDepartmentSmallGroupSetState {
	def smallGroupSet: DepartmentSmallGroupSet
	lazy val existingSet = Some(smallGroupSet)
}

class EditDepartmentSmallGroupSetCommandInternal(val smallGroupSet: DepartmentSmallGroupSet) extends ModifyDepartmentSmallGroupSetCommandInternal with EditDepartmentSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(smallGroupSet)

	def applyInternal() = transactional() {
		val autoDeregister = smallGroupSet.department.autoGroupDeregistration

		val oldUsers = Set[User]()
		// TODO FIXME
//			if (autoDeregister) membershipService.determineMembershipUsers(smallGroupSet.upstreamAssessmentGroups, Option(smallGroupSet.members)).toSet
//			else Set[User]()

		val newUsers = Set[User]()
		// TODO FIXME
//			if (autoDeregister) membershipService.determineMembershipUsers(linkedUpstreamAssessmentGroups, Option(members)).toSet
//			else Set[User]()

		copyTo(smallGroupSet)

		// TAB-1561
		if (autoDeregister) {
			// Wrap removal in a sub-command so that we can do auditing
			for {
				user <- oldUsers -- newUsers
				group <- smallGroupSet.groups.asScala
				if (group.students.includesUser(user))
			} null // TODO FIXME removeFromGroupCommand(user, group).apply()
		}

		smallGroupService.saveOrUpdate(smallGroupSet)
		smallGroupSet
	}

}

abstract class ModifyDepartmentSmallGroupSetCommandInternal
	extends CommandInternal[DepartmentSmallGroupSet] with ModifyDepartmentSmallGroupSetState with BindListener {

	// start complicated membership stuff
	lazy val existingMembers: Option[UnspecifiedTypeUserGroup] = existingSet.map(_.members)

	def copyFrom(set: DepartmentSmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		allocationMethod = set.allocationMethod
		allowSelfGroupSwitching = set.allowSelfGroupSwitching
		defaultMaxGroupSizeEnabled = set.defaultMaxGroupSizeEnabled
		defaultMaxGroupSize = set.defaultMaxGroupSize

		groups.clear()
		groups.addAll(set.groups.asScala.map(ModifyDepartmentSmallGroupCommand.edit).asJava)
	}

	def copyTo(set: DepartmentSmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.allocationMethod = allocationMethod

		set.allowSelfGroupSwitching = allowSelfGroupSwitching
		set.defaultMaxGroupSizeEnabled = defaultMaxGroupSizeEnabled
		set.defaultMaxGroupSize = defaultMaxGroupSize

		// Clear the groups on the set and add the result of each command; this may result in a new group or an existing one.
		// TAB-2304 Don't do a .clear() and .addAll() because that confuses Hibernate
		val newGroups = groups.asScala.filter(!_.delete).map(_.apply())
		set.groups.asScala.filterNot(newGroups.contains).foreach(set.groups.remove)
		newGroups.filterNot(set.groups.contains).foreach(set.groups.add)

		if (set.members == null) set.members = UserGroup.ofUniversityIds
// TODO FIXME		set.members.copyFrom(members)
	}

	override def onBind(result: BindingResult) {
		// If the last element of groups is both a Creation and is empty, disregard it
		def isEmpty(cmd: ModifyDepartmentSmallGroupState) = cmd match {
			case cmd: CreateDepartmentSmallGroupCommandState if !cmd.name.hasText => true
			case _ => false
		}

		while (!groups.isEmpty() && isEmpty(groups.asScala.last))
			groups.remove(groups.asScala.last)
	}

}

trait ModifyDepartmentSmallGroupSetCommandValidation extends SelfValidating {
	self: ModifyDepartmentSmallGroupSetState =>

	override def validate(errors: Errors) {
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")

		if (allocationMethod == null) errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.NotEmpty")

		groups.asScala.zipWithIndex foreach { case (cmd, index) =>
			errors.pushNestedPath("groups[" + index + "]")
			cmd.validate(errors)
			errors.popNestedPath()
		}
	}
}

trait CreateDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(department))
	}
}

trait CreateDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: CreateDepartmentSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.department(department).properties("name" -> name)
	}

	override def describeResult(d: Description, set: DepartmentSmallGroupSet) =
		d.department(set.department).properties("smallGroupSet" -> set.id)
}

trait EditDepartmentSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditDepartmentSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(smallGroupSet))
	}
}

trait EditDepartmentSmallGroupSetDescription extends Describable[DepartmentSmallGroupSet] {
	self: EditDepartmentSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.department(smallGroupSet.department).properties("smallGroupSet" -> smallGroupSet.id)
	}

}