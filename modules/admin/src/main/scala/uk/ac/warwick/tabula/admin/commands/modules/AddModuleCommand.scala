package uk.ac.warwick.tabula.admin.commands.modules

import uk.ac.warwick.tabula.data.Transactions._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model._
import collection.JavaConversions._
import org.springframework.validation.{ ValidationUtils, Errors }
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}

object AddModuleCommand {
	def apply(department: Department) =
		new AddModuleCommandInternal(department)
				with ComposableCommand[Module]
				with AddModuleCommandPermissions
				with AddModuleCommandValidation
				with AddModuleCommandDescription
				with AutowiringModuleAndDepartmentServiceComponent
}

class AddModuleCommandInternal(val department: Department) extends CommandInternal[Module] with AddModuleCommandState {
	self: ModuleAndDepartmentServiceComponent =>

	def applyInternal() = transactional() {
		val module = new Module()
		module.department = department
		module.name = name
		module.code = sanitisedCode

		moduleAndDepartmentService.saveOrUpdate(module)
		module
	}

}

trait AddModuleCommandValidation extends SelfValidating {
	self: AddModuleCommandState with ModuleAndDepartmentServiceComponent =>

	def validate(errors: Errors) {
		// TODO proper department checks before we open this up to non-sysadmins

		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "code", "NotEmpty")
		ValidationUtils.rejectIfEmptyOrWhitespace(errors, "name", "NotEmpty")

		// check for duplicate name or code
		if (!errors.hasErrors) {
			if (moduleAndDepartmentService.getModuleByCode(sanitisedCode).isDefined) {
				errors.rejectValue("code", "code.duplicate.module", Array(code.toUpperCase), "")
			}
			if (department.modules.exists { _.name equalsIgnoreCase name }) {
				errors.rejectValue("name", "name.duplicate.module", Array(name), "")
			}
		}
	}
}

trait AddModuleCommandState {
	def department: Department

	var code: String = _
	var name: String = _

	def sanitisedCode = Option(code).map(_.toLowerCase.trim).orNull
}

trait AddModuleCommandPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: AddModuleCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Module.Create, mandatory(department))
	}
}

trait AddModuleCommandDescription extends Describable[Module] {
	self: AddModuleCommandState =>

	def describe(d: Description) =
		d.department(department)
		 .properties(
				"moduleCode" -> sanitisedCode,
				"moduleName" -> name
			)
}