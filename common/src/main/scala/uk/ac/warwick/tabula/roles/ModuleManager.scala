package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions.{Profiles, _}

case class DepartmentModuleManager(department: model.Department) extends BuiltInRole(ModuleManagerRoleDefinition, department)

case class ModuleManager(module: model.Module) extends BuiltInRole(ModuleManagerRoleDefinition, module)

case object ModuleManagerRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Module Manager"

	GeneratesSubRole(ModuleAssistantRoleDefinition)

	GrantsScopedPermission(
		Assignment.Archive,
		Assignment.Delete,

		Submission.SendReceipt,
		Submission.Delete,

		AssignmentFeedback.Publish,

		SmallGroups.Archive,
		SmallGroups.Delete,
		SmallGroups.ImportFromExternalSystem,

		Profiles.Read.ModuleRegistration.Core,
		Profiles.Read.ModuleRegistration.Results
	)

	def canDelegateThisRolesPermissions: JBoolean = true
}