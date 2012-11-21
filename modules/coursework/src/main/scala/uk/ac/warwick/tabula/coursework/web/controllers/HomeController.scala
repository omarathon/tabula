package uk.ac.warwick.tabula.coursework.web.controllers
import org.springframework.stereotype.Controller
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.helpers.ArrayList

@Controller class HomeController extends CourseworkController {
	var moduleService = Wire.auto[ModuleAndDepartmentService]
	var assignmentService = Wire.auto[AssignmentService]
	var userLookup = Wire.auto[UserLookupService]
	var features = Wire.auto[Features]
	def groupService = userLookup.getGroupService

	hideDeletedItems

	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		if (user.loggedIn) {
			val ownedDepartments = moduleService.departmentsOwnedBy(user.idForPermissions)
			val ownedModules = moduleService.modulesManagedBy(user.idForPermissions)

			val assignmentsWithFeedback = assignmentService.getAssignmentsWithFeedback(user.universityId)
			val enrolledAssignments = 
				if (features.assignmentMembership) assignmentService.getEnrolledAssignments(user.apparentUser)
				else Seq.empty
			val assignmentsWithSubmission =
				if (features.submissions) assignmentService.getAssignmentsWithSubmission(user.universityId)
				else Seq.empty
				
			// exclude assignments already included in other lists.
			val enrolledAssignmentsTrimmed = enrolledAssignments.diff(assignmentsWithFeedback).diff(assignmentsWithSubmission)
			// adorn the enrolled assignments with extra data.
			val enrolledAssignmentsInfo = for (assignment <- enrolledAssignmentsTrimmed) yield {
				val extension = assignment.extensions.find(_.userId == user.apparentId)
				val isExtended = assignment.isWithinExtension(user.apparentId)
				val extensionRequested = extension.isDefined && !extension.get.isManual
				Map(
					"assignment" -> assignment,
					"extension" -> extension,
					"isExtended" -> isExtended,
					"extensionRequested" -> extensionRequested,
					"submittable" -> assignment.submittable(user.apparentId)
				)
			}

			Mav("home/view",
				"assignmentsWithFeedback" -> assignmentsWithFeedback,
				"enrolledAssignments" -> enrolledAssignmentsInfo,
				"assignmentsWithSubmission" -> assignmentsWithSubmission.diff(assignmentsWithFeedback),
				"ownedDepartments" -> ownedDepartments,
				"ownedModule" -> ownedModules,
				"ownedModuleDepartments" -> ownedModules.map { _.department }.distinct)

		} else {
			Mav("home/view")
		}
	}

	def webgroupsToMap(groups: Seq[Group]) = groups
		.map { (g: Group) => (Module.nameFromWebgroupName(g.getName), g) }
		.sortBy { _._1 }

}