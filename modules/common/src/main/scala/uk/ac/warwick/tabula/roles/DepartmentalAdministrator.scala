package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsSelector
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
		Masquerade,

		Department.ManageExtensionSettings,
		Department.ManageDisplaySettings,
		Department.DownloadFeedbackReport,

		Department.ArrangeModules,
		Department.ArrangeRoutes,

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

		MonitoringPoints.Report,

		MemberNotes.Delete,

		Profiles.MeetingRecord.ReadDetails(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.ScheduledMeetingRecord.Create(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.ScheduledMeetingRecord.Update,
		Profiles.ScheduledMeetingRecord.Delete,

		Profiles.MeetingRecord.Create(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.MeetingRecord.Update(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.MeetingRecord.Delete(PermissionsSelector.Any[StudentRelationshipType]),

		// TAB-1878
		Profiles.Read.TelephoneNumber,
		Profiles.Read.MobileNumber,

		SmallGroups.Read
	)
	def canDelegateThisRolesPermissions:JBoolean = true

}
