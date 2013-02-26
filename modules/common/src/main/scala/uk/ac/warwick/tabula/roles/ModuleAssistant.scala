package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

case class ModuleAssistant(module: model.Module) extends BuiltInRole(module, ModuleAssistantRoleDefinition)

case object ModuleAssistantRoleDefinition extends BuiltInRoleDefinition {
		
	GrantsScopedPermission( 
		Module.Read,
		
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
		Feedback.Delete
	)

}