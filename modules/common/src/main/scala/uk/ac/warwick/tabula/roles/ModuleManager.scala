package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget

case class DepartmentModuleManager(department: model.Department) extends BuiltInRole(department, ModuleManagerRoleDefinition)
case class ModuleManager(module: model.Module) extends BuiltInRole(module, ModuleManagerRoleDefinition)

case object ModuleManagerRoleDefinition extends BuiltInRoleDefinition {
	
	GrantsScopedPermission( 
		Module.Read,
		
		Assignment.Archive,
		Assignment.Create,
		Assignment.Read,
		Assignment.Update,
		Assignment.Delete,
		
		Submission.ViewPlagiarismStatus,
		Submission.ManagePlagiarismStatus,
		Submission.CheckForPlagiarism,
		Submission.ReleaseForMarking,
		// No Submission.Create() here for obvious reasons!		
		Submission.Read,
		Submission.Update,
		Submission.Delete,
		
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
		
		Feedback.Publish,
		Feedback.Create,
		Feedback.Read,
		Feedback.Update,
		Feedback.Delete
	)

}