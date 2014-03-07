package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.model.forms.{ExtensionState, Extension}
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.ItemNotFoundException

class ListExtensionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser)
	extends Command[Seq[ExtensionGraph]] with ReadOnly with Unaudited {

	mustBeLinked(mandatory(assignment), mandatory(module))
	PermissionCheck(Permissions.Extension.Read, assignment)

	if (assignment.openEnded) throw new ItemNotFoundException

	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	var userLookup = Wire.auto[UserLookupService]

	def applyInternal() = {
		val assignmentUsers = assignmentMembershipService.determineMembershipUsers(assignment)

		val assignmentMembership = Map() ++ (
			for(assignmentUser <- assignmentUsers)
				yield (assignmentUser.getWarwickId -> assignmentUser)
		)

		val extensionsByState = assignment.extensions.toSeq.groupBy(_.state)

		// all the users that aren't members of this assignment, but have submitted work to it
		val extensionsFromNonMembers = assignment.extensions.filterNot(x => assignmentMembership.contains(x.universityId))
		val nonMembers = userLookup.getUsersByWarwickUniIds(extensionsFromNonMembers.map { _.universityId })

		// build lookup of names from non members of the assignment that have submitted work plus members
		val students = nonMembers ++ assignmentMembership

		(for (student <- students) yield {
			// deconstruct the map, bleh
			val universityId = student._1
			val user = student._2
			val hasOutstandingExtensionRequest = extensionsByState.getOrElse(ExtensionState.Unreviewed, Seq.empty).map(_.universityId).contains(universityId)
			val hasApprovedExtension = extensionsByState.getOrElse(ExtensionState.Approved, Seq.empty).map(_.universityId).contains(universityId)
			val hasRejectedExtension = extensionsByState.getOrElse(ExtensionState.Rejected, Seq.empty).map(_.universityId).contains(universityId)
			val extension = assignment.extensions.find(_.universityId == universityId)

			new ExtensionGraph(universityId, user, hasOutstandingExtensionRequest, hasApprovedExtension, hasRejectedExtension, extension)
		}).toSeq
	}
}

case class ExtensionGraph(
	universityId: String,
	user: User,
	hasOutstandingExtensionRequest: Boolean,
	hasApprovedExtension: Boolean,
	hasRejectedExtension: Boolean,
	extension: Option[Extension])