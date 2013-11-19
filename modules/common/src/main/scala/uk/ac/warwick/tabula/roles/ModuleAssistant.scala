package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.permissions.PermissionsTarget
import uk.ac.warwick.tabula.JavaImports

case class ModuleAssistant(module: model.Module) extends BuiltInRole(ModuleAssistantRoleDefinition, module)

case object ModuleAssistantRoleDefinition extends BuiltInRoleDefinition {
	
	override def description = "Module Assistant"
	
	GeneratesSubRole(ModuleAuditorRoleDefinition)

	GrantsScopedPermission(		
		RolesAndPermissions.Read,
		
		Assignment.Create,
		Assignment.Update,
		
		Submission.ManagePlagiarismStatus,
		Submission.CheckForPlagiarism,
		Submission.ReleaseForMarking,
		// No Submission.Create() here for obvious reasons!		
		Submission.Update,
		
		Marks.Create,
		Marks.Update,
		Marks.Delete,
		
		Extension.ReviewRequest,
		Extension.Create,
		Extension.Update,
		Extension.Delete,
		
		Feedback.Create,
		Feedback.Update,
		Feedback.Delete,
		
		SmallGroups.Create,
		SmallGroupEvents.Register,
		SmallGroups.Update,
		SmallGroups.Allocate
	)

	def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}