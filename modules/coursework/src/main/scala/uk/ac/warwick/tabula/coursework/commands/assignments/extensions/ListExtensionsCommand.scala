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
import org.joda.time.{Days, DateTime}
import uk.ac.warwick.tabula.coursework.web.Routes.admin.assignment.extension

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

		// all the users that aren't members of this assignment, but have submitted work to it
		val extensionsFromNonMembers = assignment.extensions.filterNot(x => assignmentMembership.contains(x.universityId))
		val nonMembers = userLookup.getUsersByWarwickUniIds(extensionsFromNonMembers.map { _.universityId })

		// build lookup of names from non members of the assignment that have submitted work plus members
		val students = nonMembers ++ assignmentMembership

		(for (student <- students) yield {
			// deconstruct the map, bleh
			val universityId = student._1
			val user = student._2
			val extension = assignment.extensions.find(_.universityId == universityId)
			val isAwaitingReview = extension exists (_.awaitingReview)
			val hasApprovedExtension = extension exists (_.approved)
			val hasRejectedExtension = extension exists (_.rejected)

			// use real days not working days, for displayed duration, as markers need to know how late it *actually* would be after deadline
			val duration = extension match {
				case Some(e) if e.expiryDate != null => Days.daysBetween(assignment.closeDate, e.expiryDate).getDays
			case _ => 0
		}
			val requestedExtraDuration = extension match {
				case Some(e) if e.requestedExpiryDate != null && e.expiryDate != null => Days.daysBetween(e.expiryDate, e.requestedExpiryDate).getDays
				case Some(e) if e.requestedExpiryDate != null && e.expiryDate == null => Days.daysBetween(assignment.closeDate, e.requestedExpiryDate).getDays
				case _ => 0
			}

			new ExtensionGraph(universityId, user, isAwaitingReview, hasApprovedExtension, hasRejectedExtension, duration, requestedExtraDuration, extension)
		}).toSeq
	}
}

case class ExtensionGraph(
	universityId: String,
	user: User,
	isAwaitingReview: Boolean,
	hasApprovedExtension: Boolean,
	hasRejectedExtension: Boolean,
	duration: Int,
	requestedExtraDuration: Int,
	extension: Option[Extension])