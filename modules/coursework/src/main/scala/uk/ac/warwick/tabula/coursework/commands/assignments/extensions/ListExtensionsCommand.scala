package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.userlookup.User
import uk.ac.warwick.tabula.ItemNotFoundException

class ListExtensionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser)
	extends Command[ExtensionInformation] with ReadOnly with Unaudited {

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

		val manualExtensions = assignment.extensions.filter(_.requestedOn == null)
		val isExtensionManager = module.department.isExtensionManager(user.apparentId)
		val extensionRequests = assignment.extensions.filterNot(manualExtensions contains(_))

		// all the users that aren't members of this assignment, but have submitted work to it
		val extensionsFromNonMembers = assignment.extensions.filterNot(x => assignmentMembership.contains(x.universityId))
		val nonMembers = extensionsFromNonMembers.par.map { extension => 
			(extension.universityId -> userLookup.getUserByWarwickUniId(extension.universityId))
		}.seq.toMap

		// build lookup of names from non members of the assignment that have submitted work plus members
		val students = nonMembers ++ assignmentMembership

		// users that are members of the assignment but have not yet requested or been granted an extension
		val potentialExtensions =
			assignmentMembership.keySet -- (manualExtensions.map(_.universityId).toSet) --
				(extensionRequests.map(_.universityId).toSet)

		new ExtensionInformation(
			students,
			manualExtensions,
			extensionRequests,
			isExtensionManager,
			potentialExtensions
		)
	}

}

case class ExtensionInformation(
	students: Map[String, User],
	manualExtensions: Seq[Extension],
	extensionRequests: Seq[Extension],
	isExtensionManager: Boolean,
	potentialExtensions: Set[String])