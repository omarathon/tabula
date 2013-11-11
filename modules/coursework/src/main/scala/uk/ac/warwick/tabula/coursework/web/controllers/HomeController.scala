package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.HomeControllerCollectionsHelper._
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.ModuleAndDepartmentService
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.userlookup.Group
import collection.JavaConversions._
import uk.ac.warwick.tabula.data.model.{Assignment, Module}
import uk.ac.warwick.tabula.services.UserLookupService
import uk.ac.warwick.tabula.services.AssignmentService
import uk.ac.warwick.tabula.Features
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.services.ActivityService
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.PermissionsService

@Controller class HomeController extends CourseworkController {
	var moduleService = Wire[ModuleAndDepartmentService]
	var assignmentService = Wire[AssignmentService]
	var assignmentMembershipService = Wire[AssignmentMembershipService]
	var activityService = Wire[ActivityService]
	var permissionsService = Wire[PermissionsService]

	var userLookup = Wire[UserLookupService]
	var features = Wire[Features]
	def groupService = userLookup.getGroupService

	hideDeletedItems

	@RequestMapping(Array("/api/activity/pagelet/{doc}/{field}/{token}"))
	def pagelet(user: CurrentUser, @PathVariable("doc") doc: Int, @PathVariable("field") field: Long, @PathVariable("token") token: Long) = {
		if (user.loggedIn) {
			try {
				val pagedActivities = activityService.getNoteworthySubmissions(user, doc, field, token)

				Mav("home/activities",
					"activities" -> pagedActivities,
					"async" -> true).noLayout
			}
			catch {
				case e:IllegalStateException => {
					Mav("home/activities",
					"expired" -> true).noLayout
				}
			}
		} else {
			Mav("home/empty").noLayout
		}
	}

	@RequestMapping(Array("/")) def home(user: CurrentUser) = {
		if (user.loggedIn) {
			val ownedDepartments = moduleService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
			val ownedModules = moduleService.modulesWithPermission(user, Permissions.Module.ManageAssignments)

			val pagedActivities = activityService.getNoteworthySubmissions(user)

			val assignmentsForMarking = assignmentService.getAssignmentWhereMarker(user.apparentUser).sortBy(_.closeDate)
			// add the number of submissions to each assignment for marking
			val assignmentsForMarkingInfo = for (assignment <- assignmentsForMarking) yield {
				val submissions = assignment.getMarkersSubmissions(user.apparentUser)
				Map(
					"assignment" -> assignment,
					"numSubmissions" -> submissions.size,
					"isAdmin" -> securityService.can(user, Permissions.Module.ManageAssignments, assignment)
				)
			}

			val assignmentsWithFeedback = assignmentService.getAssignmentsWithFeedback(user.universityId)

			val enrolledAssignments =
				if (features.assignmentMembership) assignmentMembershipService.getEnrolledAssignments(user.apparentUser)
				else Seq.empty
			val assignmentsWithSubmission =
				if (features.submissions) assignmentService.getAssignmentsWithSubmission(user.universityId)
				else Seq.empty
			val lateFormativeAssignments = enrolledAssignments.filter { ass => !ass.summative && ass.isClosed } // TAB-706

			// exclude assignments already included in other lists.
			val enrolledAssignmentsTrimmed =
				enrolledAssignments
					.diff(assignmentsWithFeedback)
					.diff(assignmentsWithSubmission)
					.filter {_.collectSubmissions} // TAB-475
					.filterNot(lateFormativeAssignments.contains(_))
					.sortWith { (ass1, ass2) =>
						// TAB-569 personal time to deadline - if ass1 is "due" before ass2 for the current user
						// Show open ended assignments after
						if (ass2.openEnded && !ass1.openEnded) true
						else if (ass1.openEnded && !ass2.openEnded) false
						else {
							def timeToDeadline(ass: Assignment) = {
								val extension = ass.extensions.find(_.userId == user.apparentId)
								val isExtended = ass.isWithinExtension(user.apparentId)

								if (ass.openEnded) ass.openDate
								else if (isExtended) (extension map { _.expiryDate }).get
								else ass.closeDate
							}

							timeToDeadline(ass1) < timeToDeadline(ass2)
						}
					}

			def enhanced(assignment: Assignment) = {
				val extension = assignment.extensions.find(_.userId == user.apparentId)
				val isExtended = assignment.isWithinExtension(user.apparentId)
				val extensionRequested = extension.isDefined && !extension.get.isManual
				val submission = assignment.submissions.find(_.universityId == user.universityId)
				val feedback = assignment.feedbacks.filter(_.released).find(_.universityId == user.universityId)
				Map(
					"assignment" -> assignment,
					"submission" -> submission,
					"hasSubmission" -> submission.isDefined,
					"feedback" -> feedback,
					"hasFeedback" -> feedback.isDefined,
					"hasExtension" -> extension.isDefined,
					"extension" -> extension,
					"isExtended" -> isExtended,
					"extensionRequested" -> extensionRequested,
					"submittable" -> assignment.submittable(user.apparentId),
					"resubmittable" -> assignment.resubmittable(user.apparentId),
					"closed" -> assignment.isClosed,
					"summative" -> assignment.summative.booleanValue
				)
			}

			// adorn the enrolled assignments with extra data.
			val enrolledAssignmentsInfo = for (assignment <- enrolledAssignmentsTrimmed) yield enhanced(assignment)
			val assignmentsWithFeedbackInfo = for (assignment <- assignmentsWithFeedback) yield enhanced(assignment)
			val assignmentsWithSubmissionInfo = for (assignment <- assignmentsWithSubmission.diff(assignmentsWithFeedback)) yield enhanced(assignment)
			val lateFormativeAssignmentsInfo = for (assignment <- lateFormativeAssignments) yield enhanced(assignment)

			Mav("home/view",
				"enrolledAssignments" -> enrolledAssignmentsInfo,
				"historicAssignments" -> getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo, assignmentsWithSubmissionInfo, lateFormativeAssignmentsInfo),

				"assignmentsForMarking" -> assignmentsForMarkingInfo,
				"ownedDepartments" -> ownedDepartments,
				"ownedModule" -> ownedModules,
				"ownedModuleDepartments" -> ownedModules.map { _.department },
				"activities" -> pagedActivities)
		} else {
			Mav("home/view")
		}

	}

	def webgroupsToMap(groups: Seq[Group]) = groups
		.map { (g: Group) => (Module.nameFromWebgroupName(g.getName), g) }
		.sortBy { _._1 }
}

//
// Stateless functions for collection generation
//
object HomeControllerCollectionsHelper {

	type AssignmentInfo = Map[String, Any]

	def getHistoricAssignmentsInfo(assignmentsWithFeedbackInfo: Seq[AssignmentInfo], assignmentsWithSubmissionInfo: Seq[AssignmentInfo], lateFormativeAssignmentsInfo: Seq[AssignmentInfo]): Seq[AssignmentInfo] = {
		assignmentsWithFeedbackInfo
			.union(assignmentsWithSubmissionInfo)
			.union(lateFormativeAssignmentsInfo)
			.sortWith {	(info1, info2) =>
			def toDate(info: AssignmentInfo) = {
				val assignment = info("assignment").asInstanceOf[Assignment]
				val submission = info("submission").asInstanceOf[Option[Submission]]

				submission map { _.submittedDate } getOrElse { if (assignment.openEnded) assignment.openDate else assignment.closeDate }
			}

			toDate(info1) < toDate(info2)
		}.distinct.reverse
	}

}
