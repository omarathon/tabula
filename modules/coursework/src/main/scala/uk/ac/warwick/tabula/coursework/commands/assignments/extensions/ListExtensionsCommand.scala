package uk.ac.warwick.tabula.coursework.commands.assignments.extensions

import scala.collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.permissions._
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.data.model.forms.Extension
import uk.ac.warwick.tabula.services.AssignmentMembershipService

class ListExtensionsCommand(val module: Module, val assignment: Assignment, val user: CurrentUser) extends Command[ExtensionInformation] with ReadOnly with Unaudited {
	
	mustBeLinked(assignment,module)
	PermissionCheck(Permissions.Extension.Read, assignment)
	
	var assignmentMembershipService = Wire.auto[AssignmentMembershipService]
	var userLookup = Wire.auto[UserLookupService]
	
	def applyInternal() = {
		val assignmentUsers = assignmentMembershipService.determineMembershipUsers(assignment)
		
		val assignmentMembership = Map() ++ (
			for(assignmentUser <- assignmentUsers)  
				yield (assignmentUser.getWarwickId -> assignmentUser.getFullName())	
		)
			
		val manualExtensions = assignment.extensions.filter(_.requestedOn == null)
		val isExtensionManager = module.department.isExtensionManager(user.apparentId)
		val extensionRequests = assignment.extensions.filterNot(manualExtensions contains(_))

		// all the users that aren't members of this assignment, but have submitted work to it
		val extensionsFromNonMembers = assignment.extensions.filterNot(x => assignmentMembership.contains(x.getUniversityId))	
		val nonMembers = Map() ++ (
			for(extension <- extensionsFromNonMembers) 
				yield (extension.getUniversityId -> userLookup.getUserByWarwickUniId(extension.getUniversityId).getFullName()) 
		)
		
		// build lookup of names from non members of the assignment that have submitted work plus members 
		val studentNameLookup = nonMembers ++ assignmentMembership
		
		// users that are members of the assignment but have not yet requested or been granted an extension
		val potentialExtensions =
			assignmentMembership.keySet -- (manualExtensions.map(_.universityId).toSet) --
				(extensionRequests.map(_.universityId).toSet)
				
		new ExtensionInformation(
			studentNameLookup,
			manualExtensions,
			extensionRequests,
			isExtensionManager,
			potentialExtensions
		)
	}

}

case class ExtensionInformation(
	studentNames: Map[String, String],
	manualExtensions: Seq[Extension],
	extensionRequests: Seq[Extension],
	isExtensionManager: Boolean,
	potentialExtensions: Set[String])