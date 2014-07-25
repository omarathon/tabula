package uk.ac.warwick.tabula.groups.commands.admin

import org.springframework.validation.{BindingResult, Errors}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.data.model.groups._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.system.BindListener
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.helpers.StringUtils._

object ModifySmallGroupSetCommand {
	def create(module: Module) =
		new CreateSmallGroupSetCommandInternal(module)
			with ComposableCommand[SmallGroupSet]
			with SetDefaultSmallGroupSetName
			with CreateSmallGroupSetPermissions
			with CreateSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with AutowiringSmallGroupServiceComponent

	def edit(module: Module, set: SmallGroupSet) =
		new EditSmallGroupSetCommandInternal(module, set)
			with ComposableCommand[SmallGroupSet]
			with EditSmallGroupSetPermissions
			with EditSmallGroupSetDescription
			with ModifySmallGroupSetValidation
			with AutowiringSmallGroupServiceComponent
}

trait ModifySmallGroupSetCommandState extends CurrentAcademicYear {
	def module: Module
	def existingSet: Option[SmallGroupSet]

	var name: String = _

	var format: SmallGroupFormat = _

	var allocationMethod: SmallGroupAllocationMethod = SmallGroupAllocationMethod.Manual

	var allowSelfGroupSwitching: Boolean = true
	var studentsCanSeeTutorName: Boolean = false
	var studentsCanSeeOtherMembers: Boolean = false

	var collectAttendance: Boolean = true

	var linkedDepartmentSmallGroupSet: DepartmentSmallGroupSet = _
}

trait CreateSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	val existingSet = None
}

trait EditSmallGroupSetCommandState extends ModifySmallGroupSetCommandState {
	def set: SmallGroupSet
	def existingSet = Some(set)
}

class CreateSmallGroupSetCommandInternal(val module: Module) extends ModifySmallGroupSetCommandInternal with CreateSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	override def applyInternal() = transactional() {
		val set = new SmallGroupSet(module)
		copyTo(set)
		smallGroupService.saveOrUpdate(set)
		set
	}
}

trait SetDefaultSmallGroupSetName extends BindListener {
	self: CreateSmallGroupSetCommandState =>

	override def onBind(result: BindingResult) {
		// If we haven't set a name, make one up
		if (!name.hasText) {
			Option(format).foreach { format => name = "%s %ss".format(module.code.toUpperCase, format) }
		}
	}
}

class EditSmallGroupSetCommandInternal(val module: Module, val set: SmallGroupSet) extends ModifySmallGroupSetCommandInternal with EditSmallGroupSetCommandState {
	self: SmallGroupServiceComponent =>

	copyFrom(set)

	override def applyInternal() = transactional() {
		copyTo(set)
		smallGroupService.saveOrUpdate(set)
		set
	}
}

abstract class ModifySmallGroupSetCommandInternal extends CommandInternal[SmallGroupSet] with ModifySmallGroupSetCommandState {
	def copyFrom(set: SmallGroupSet) {
		name = set.name
		academicYear = set.academicYear
		format = set.format
		allocationMethod = set.allocationMethod
		allowSelfGroupSwitching = set.allowSelfGroupSwitching
		studentsCanSeeTutorName = set.studentsCanSeeTutorName
		studentsCanSeeOtherMembers = set.studentsCanSeeOtherMembers
		collectAttendance = set.collectAttendance
		linkedDepartmentSmallGroupSet = set.linkedDepartmentSmallGroupSet
	}

	def copyTo(set: SmallGroupSet) {
		set.name = name
		set.academicYear = academicYear
		set.format = format
		set.allocationMethod = allocationMethod
		set.collectAttendance = collectAttendance

		set.allowSelfGroupSwitching = allowSelfGroupSwitching
		set.studentsCanSeeOtherMembers = studentsCanSeeOtherMembers
		set.studentsCanSeeTutorName = studentsCanSeeTutorName

		set.linkedDepartmentSmallGroupSet = linkedDepartmentSmallGroupSet
	}
}

trait ModifySmallGroupSetValidation extends SelfValidating {
	self: ModifySmallGroupSetCommandState =>

	override def validate(errors: Errors) {
		if (!name.hasText) errors.rejectValue("name", "smallGroupSet.name.NotEmpty")
		else if (name.orEmpty.length > 200) errors.rejectValue("name", "smallGroupSet.name.Length", Array[Object](200: JInteger), "")

		if (format == null) errors.rejectValue("format", "smallGroupSet.format.NotEmpty")
		if (allocationMethod == null) errors.rejectValue("allocationMethod", "smallGroupSet.allocationMethod.NotEmpty")

		existingSet.foreach { set =>
			if (academicYear != set.academicYear) errors.rejectValue("academicYear", "smallGroupSet.academicYear.cantBeChanged")
		}
	}
}

trait CreateSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: CreateSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.SmallGroups.Create, mandatory(module))
	}
}

trait CreateSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: CreateSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.module(module).properties("name" -> name)
	}

	override def describeResult(d: Description, set: SmallGroupSet) =
		d.smallGroupSet(set)
}

trait EditSmallGroupSetPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: EditSmallGroupSetCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		mustBeLinked(set, module)
		p.PermissionCheck(Permissions.SmallGroups.Update, mandatory(set))
	}
}

trait EditSmallGroupSetDescription extends Describable[SmallGroupSet] {
	self: EditSmallGroupSetCommandState =>

	override def describe(d: Description) {
		d.smallGroupSet(set)
	}

}