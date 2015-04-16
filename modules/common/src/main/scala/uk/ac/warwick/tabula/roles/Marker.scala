package uk.ac.warwick.tabula.roles

import uk.ac.warwick.tabula.permissions.Permissions._
import uk.ac.warwick.tabula.data.model.{Assessment, Assignment}
import uk.ac.warwick.tabula.JavaImports

case class Marker(assessment: Assessment) extends BuiltInRole(MarkerRoleDefinition, assessment)

case object MarkerRoleDefinition extends UnassignableBuiltInRoleDefinition {
	
	override def description = "Marker"
	
	GrantsScopedPermission(
		Feedback.Create,
		Feedback.Read,
		Marks.Create,
		Marks.Read,
		Marks.DownloadOwnTemplate,
		Submission.Read,
		Submission.ViewPlagiarismStatus
	)

}