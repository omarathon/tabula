package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports

case class ModuleAssistant(module: model.Module) extends BuiltInRole(ModuleAssistantRoleDefinition, module)

case object ModuleAssistantRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Module Assistant"

	GrantsScopedPermission( 
		Module.ManageAssignments,
		Module.ManageSmallGroups,
		
		RolesAndPermissions.Read,
		
		Assignment.Create,
		Assignment.Read,
		Assignment.Update,
		
		Submission.ViewPlagiarismStatus,
		Submission.ManagePlagiarismStatus,
		Submission.CheckForPlagiarism,
		Submission.ReleaseForMarking,
		// No Submission.Create() here for obvious reasons!		
		Submission.Read,
		Submission.Update,
		
		Marks.DownloadTemplate,
		Marks.Create,
		Marks.Read,
		Marks.Update,
		Marks.Delete,
		
		Extension.ReviewRequest,
		Extension.Create,
		Extension.Read,
		Extension.Update,
		Extension.Delete,
		
		Feedback.Create,
		Feedback.Read,
		Feedback.Update,
		Feedback.Delete,
		
		SmallGroups.Create,
		SmallGroups.Read,
		SmallGroups.ReadMembership,
		SmallGroups.Update,
		SmallGroups.Allocate
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}