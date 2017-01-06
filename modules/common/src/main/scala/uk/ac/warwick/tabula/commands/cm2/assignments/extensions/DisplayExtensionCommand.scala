package uk.ac.warwick.tabula.commands.cm2.assignments.extensions

import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Assignment, Submission}
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{ExtensionServiceComponent, _}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.userlookup.User

case class DisplayExtensionDetail(
	extension: Option[Extension],
	student: User,
	previousExtensions: Seq[Extension],
	previousSubmissions: Seq[Submission]
){
	def numAcceptedExtensions: Int = previousExtensions.count(_.approved)
	def numRejectedExtensions: Int = previousExtensions.count(_.rejected)
}

object DisplayExtensionCommand {
	def apply(universityId: String, assignment: Assignment) = new DisplayExtensionCommandInternal(universityId, assignment)
			with ComposableCommand[DisplayExtensionDetail]
			with DisplayExtensionPermissions
			with AutowiringUserLookupComponent
			with AutowiringExtensionServiceComponent
			with AutowiringSubmissionServiceComponent
			with ReadOnly with Unaudited
}

class DisplayExtensionCommandInternal(val universityId: String, val assignment: Assignment) extends CommandInternal[DisplayExtensionDetail]
	with DisplayExtensionState with TaskBenchmarking {

	this: UserLookupComponent with ExtensionServiceComponent with SubmissionServiceComponent  =>

	def applyInternal(): DisplayExtensionDetail = {

		val extension: Option[Extension] = assignment.findExtension(universityId)
		val user = userLookup.getUserByWarwickUniId(universityId)

		val previousExtensions = extensionService.getPreviousExtensions(user)

		val previousSubmissions = submissionService.getPreviousSubmissions(user)

		DisplayExtensionDetail(extension, user, previousExtensions, previousSubmissions)

		}
}

trait DisplayExtensionPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {
	self: DisplayExtensionState =>

	def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Assignment.Read, assignment)
	}
}

trait DisplayExtensionState {
	val assignment: Assignment
}