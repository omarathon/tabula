package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsSelector}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.JavaImports._

case class DepartmentalAdministrator(department: model.Department) extends BuiltInRole(DepartmentalAdministratorRoleDefinition, department)

case object DepartmentalAdministratorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Departmental Administrator"

	//
	// If you're removing permissions from here, please consider whether you should be
	// adding them back into UserAccessManager at the same time.
	//

	// Implicitly grants module manager role for all modules in this department, and route manager for all routes in this department
	GeneratesSubRole(ModuleManagerRoleDefinition)
	GeneratesSubRole(RouteManagerRoleDefinition)

	GrantsScopedPermission(
		Department.ManageExtensionSettings,
		Department.ManageDisplaySettings,
		Department.DownloadFeedbackReport,

		Assignment.ImportFromExternalSystem,

		FeedbackTemplate.Create,
		FeedbackTemplate.Read,
		FeedbackTemplate.Update,
		FeedbackTemplate.Delete,

		MarkingWorkflow.Create,
		MarkingWorkflow.Read,
		MarkingWorkflow.Update,
		MarkingWorkflow.Delete,

		Department.ManageProfiles,

		Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]),

		SmallGroups.Read,
		SmallGroupEvents.Register,
		SmallGroupEvents.ViewRegister
	)
	def canDelegateThisRolesPermissions:JBoolean = true

}
