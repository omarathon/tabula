package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.JavaImports
import uk.ac.warwick.tabula.data._
import uk.ac.warwick.tabula.permissions.Permissions._

case class ModuleAssistant(module: model.Module) extends BuiltInRole(ModuleAssistantRoleDefinition, module)

case object ModuleAssistantRoleDefinition extends BuiltInRoleDefinition {

  override def description = "Module Assistant"

  GeneratesSubRole(ModuleAuditorRoleDefinition)
  GeneratesSubRole(MarkerRoleDefinition)

  GrantsScopedPermission(
    RolesAndPermissions.Read,

    Assignment.Create,
    Assignment.Update,
    Assignment.MarkOnBehalf,

    Exam.Create,
    Exam.Update,

    Submission.ManagePlagiarismStatus,
    Submission.CheckForPlagiarism,
    Submission.ReleaseForMarking,
    // No Submission.Create() here for obvious reasons!
    Submission.Update,

    Extension.Create,
    Extension.Update,
    Extension.Delete,

    Feedback.Manage,
    Feedback.DownloadMarksTemplate,
    MarkerFeedback.Manage,

    SmallGroups.Create,
    SmallGroupEvents.Register,
    SmallGroups.Update,
    SmallGroups.Allocate,

    Profiles.Read.ModuleRegistration.Core
  )

  def canDelegateThisRolesPermissions: JavaImports.JBoolean = true
}
