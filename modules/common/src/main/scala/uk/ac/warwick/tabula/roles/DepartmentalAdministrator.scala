package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.{Permissions, PermissionsSelector}
import uk.ac.warwick.tabula.data.model.StudentRelationshipType
import uk.ac.warwick.tabula.JavaImports._

case class DepartmentalAdministrator(department: model.Department) extends BuiltInRole(DepartmentalAdministratorRoleDefinition, department)

case object DepartmentalAdministratorRoleDefinition extends BuiltInRoleDefinition {

	override def description = "Departmental Administrator"

	// Implicitly grants module manager role for all modules in this department
	GeneratesSubRole(ModuleManagerRoleDefinition)


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

		MonitoringPoints.View,
		MonitoringPoints.Record,
		MonitoringPoints.Manage,

		Department.ManageProfiles,

		Profiles.Read.Core,
		Profiles.Read.NextOfKin,
	  Profiles.Read.Timetable,
		Profiles.Read.StudentCourseDetails.Core,
		Profiles.Read.StudentCourseDetails.Status,
		Profiles.Read.RelationshipStudents(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.Search,

		Profiles.StudentRelationship.Create(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Read(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Update(PermissionsSelector.Any[StudentRelationshipType]),
		Profiles.StudentRelationship.Delete(PermissionsSelector.Any[StudentRelationshipType]),

		Profiles.MeetingRecord.Read(PermissionsSelector.Any[StudentRelationshipType]),

		SmallGroups.Read,
		Profiles.Read.SmallGroups,

		MemberNotes.Read,
		MemberNotes.Create,
		MemberNotes.Update,
		MemberNotes.Delete

	)
	def canDelegateThisRolesPermissions:JBoolean = true

}
