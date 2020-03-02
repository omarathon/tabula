package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.data.model.Assignment
import uk.ac.warwick.tabula.permissions.Permissions._

case class Marker(assessment: Assignment) extends BuiltInRole(MarkerRoleDefinition, assessment)

case object MarkerRoleDefinition extends UnassignableBuiltInRoleDefinition {

  override def description = "Marker"

  GrantsScopedPermission(
    Feedback.Read,
    MarkerFeedback.DownloadMarksTemplate,
    MarkerFeedback.Manage,
    Submission.Read,
    Submission.ViewPlagiarismStatus
  )

}