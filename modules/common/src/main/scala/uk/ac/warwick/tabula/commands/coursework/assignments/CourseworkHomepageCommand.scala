package uk.ac.warwick.tabula.commands.coursework.assignments

import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.coursework.assignments.CourseworkHomepageCommand._
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.ActivityService._
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.userlookup.Group

object CourseworkHomepageCommand {
	type AssignmentInfo = Map[String, Any]

	case class CourseworkHomepageInformation(
		enrolledAssignments: Seq[AssignmentInfo],
		historicAssignments: Seq[AssignmentInfo],
		assignmentsForMarking: Seq[AssignmentInfo],
		ownedDepartments: Set[Department],
		ownedModules: Set[Module],
		activities: PagedActivities
	)

	def apply(user: CurrentUser) =
		new CourseworkHomepageCommandInternal(user)
			with ComposableCommand[Option[CourseworkHomepageInformation]]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringAssessmentServiceComponent
			with AutowiringActivityServiceComponent
			with AutowiringSecurityServiceComponent
			with PubliclyVisiblePermissions with ReadOnly with Unaudited
}

class CourseworkHomepageCommandInternal(user: CurrentUser) extends CommandInternal[Option[CourseworkHomepageInformation]] with TaskBenchmarking {
	self: ModuleAndDepartmentServiceComponent with
		AssessmentServiceComponent with
		ActivityServiceComponent with
		SecurityServiceComponent =>

	def applyInternal() = {
		if (user.loggedIn) {
			val ownedDepartments = benchmarkTask("Get owned departments") {
				moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments)
			}
			val ownedModules = benchmarkTask("Get owned modules") { moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments) }

			val pagedActivities = benchmarkTask("Get noteworthy submissions") { activityService.getNoteworthySubmissions(user) }

			val assignmentsForMarking = benchmarkTask("Get assignments for marking") {
				assessmentService.getAssignmentWhereMarker(user.apparentUser).sortBy(_.closeDate)
			}
			// add the number of submissions to each assignment for marking
			val assignmentsForMarkingInfo = benchmarkTask("Get markers submissions") {
				for (assignment <- assignmentsForMarking) yield {
					val submissions = assignment.getMarkersSubmissions(user.apparentUser)
					val markerFeedbacks = submissions.flatMap( submission => assignment.getAllMarkerFeedbacks(submission.universityId, user.apparentUser))

					Map(
						"assignment" -> assignment,
						"isFeedbacksToManage" -> markerFeedbacks.nonEmpty,
						"numSubmissions" -> submissions.size,
						"marker" -> user.apparentUser,
						"isAdmin" -> securityService.can(user, Permissions.Module.ManageAssignments, assignment)
					)
				}
			}

			val courseworkInformation = StudentCourseworkFullScreenCommand(MemberOrUser(None, user.apparentUser)).apply()

			Some(CourseworkHomepageInformation(
				enrolledAssignments = courseworkInformation.enrolledAssignments,
				historicAssignments = courseworkInformation.historicAssignments,

				assignmentsForMarking = assignmentsForMarkingInfo,
				ownedDepartments = ownedDepartments,
				ownedModules = ownedModules,

				activities = pagedActivities
			))
		} else {
			None
		}
	}

	def webgroupsToMap(groups: Seq[Group]) = groups
		.map { (g: Group) => (Module.nameFromWebgroupName(g.getName), g) }
		.sortBy { _._1 }

}

object CourseworkHomepageActivityPageletCommand {
	def apply(user: CurrentUser, doc: Int, field: Long, token: Long) =
		new CourseworkHomepageActivityPageletCommandInternal(user, doc, field, token)
			with ComposableCommand[Option[PagedActivities]]
			with AutowiringActivityServiceComponent
			with PubliclyVisiblePermissions with ReadOnly with Unaudited
}

class CourseworkHomepageActivityPageletCommandInternal(user: CurrentUser, doc: Int, field: Long, token: Long) extends CommandInternal[Option[PagedActivities]] {
	self: ActivityServiceComponent =>

	def applyInternal() =
		if (user.loggedIn) Some(activityService.getNoteworthySubmissions(user, doc, field, token))
		else None
}
