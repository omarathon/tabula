package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkHomepageCommand._
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
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import uk.ac.warwick.tabula.JavaImports._
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.services.AssignmentMembershipService
import uk.ac.warwick.tabula.data.model.Submission
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.permissions.PermissionsService
import uk.ac.warwick.tabula.commands.CommandInternal
import uk.ac.warwick.tabula.commands.Unaudited
import uk.ac.warwick.tabula.commands.ReadOnly
import uk.ac.warwick.tabula.commands.ComposableCommand
import uk.ac.warwick.tabula.coursework.web.controllers.CourseworkHomepageCommand.CourseworkHomepageInformation
import uk.ac.warwick.tabula.system.permissions.Public
import uk.ac.warwick.tabula.system.permissions.PubliclyVisiblePermissions
import uk.ac.warwick.tabula.services.ModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.AssignmentServiceComponent
import uk.ac.warwick.tabula.FeaturesComponent
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.services.AutowiringActivityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentServiceComponent
import uk.ac.warwick.tabula.AutowiringFeaturesComponent
import uk.ac.warwick.tabula.services.ActivityServiceComponent
import uk.ac.warwick.tabula.services.SecurityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringSecurityServiceComponent
import uk.ac.warwick.tabula.services.AutowiringAssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.services.AssignmentMembershipServiceComponent
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.TaskBenchmarking
import uk.ac.warwick.tabula.commands.MemberOrUser

@Controller class HomeController extends CourseworkController {
	
	hideDeletedItems

	@ModelAttribute("command") def command(user: CurrentUser) = CourseworkHomepageCommand(user)

	@RequestMapping(Array("/")) def home(@ModelAttribute("command") cmd: Appliable[Option[CourseworkHomepageInformation]], user: CurrentUser) =
		cmd.apply() match {
			case Some(info) => 
				Mav("home/view",
					"student" -> MemberOrUser(user.profile, user.apparentUser),
					"enrolledAssignments" -> info.enrolledAssignments,
					"historicAssignments" -> info.historicAssignments,
					"assignmentsForMarking" -> info.assignmentsForMarking,
					"ownedDepartments" -> info.ownedDepartments,
					"ownedModule" -> info.ownedModules,
					"ownedModuleDepartments" -> info.ownedModules.map { _.department },
					"activities" -> info.activities)
			case _ => Mav("home/view")
		}
}

object CourseworkHomepageCommand {
	type AssignmentInfo = Map[String, Any] 
	
	case class CourseworkHomepageInformation(
		val enrolledAssignments: Seq[AssignmentInfo],
		val historicAssignments: Seq[AssignmentInfo],
		val assignmentsForMarking: Seq[AssignmentInfo],
		val ownedDepartments: Set[Department],
		val ownedModules: Set[Module],
		val activities: PagedActivities
	)
	
	def apply(user: CurrentUser) =
		new CourseworkHomepageCommandInternal(user)
			with ComposableCommand[Option[CourseworkHomepageInformation]]
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringAssignmentServiceComponent
			with AutowiringActivityServiceComponent
			with AutowiringSecurityServiceComponent
			with PubliclyVisiblePermissions with ReadOnly with Unaudited
}

class CourseworkHomepageCommandInternal(user: CurrentUser) extends CommandInternal[Option[CourseworkHomepageInformation]] with TaskBenchmarking {
	self: ModuleAndDepartmentServiceComponent with 
		  AssignmentServiceComponent with 
		  ActivityServiceComponent with 
		  SecurityServiceComponent =>
	
	def applyInternal() = {
		if (user.loggedIn) {
			val ownedDepartments = benchmarkTask("Get owned departments") { moduleAndDepartmentService.departmentsWithPermission(user, Permissions.Module.ManageAssignments) }
			val ownedModules = benchmarkTask("Get owned modules") { moduleAndDepartmentService.modulesWithPermission(user, Permissions.Module.ManageAssignments) }

			val pagedActivities = benchmarkTask("Get noteworthy submissions") { activityService.getNoteworthySubmissions(user) }

			val assignmentsForMarking = benchmarkTask("Get assignments for marking") { assignmentService.getAssignmentWhereMarker(user.apparentUser).sortBy(_.closeDate) }
			// add the number of submissions to each assignment for marking
			val assignmentsForMarkingInfo = benchmarkTask("Get markers submissions") { 
				for (assignment <- assignmentsForMarking) yield {
					val submissions = assignment.getMarkersSubmissions(user.apparentUser)
					Map(
						"assignment" -> assignment,
						"numSubmissions" -> submissions.size,
						"isAdmin" -> securityService.can(user, Permissions.Module.ManageAssignments, assignment)
					)
				}
			}
			
			val courseworkInformation = StudentCourseworkCommand(MemberOrUser(None, user.apparentUser)).apply()

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

@Controller class HomeActivitiesPageletController extends CourseworkController {
	
	hideDeletedItems
	
	@ModelAttribute("command") def command(user: CurrentUser, @PathVariable("doc") doc: Int, @PathVariable("field") field: Long, @PathVariable("token") token: Long) =
		CourseworkHomepageActivityPageletCommand(user, doc, field, token)

	@RequestMapping(Array("/api/activity/pagelet/{doc}/{field}/{token}"))
	def pagelet(@ModelAttribute("command") cmd: Appliable[Option[PagedActivities]]) = {
		try {
			cmd.apply() match {
				case Some(pagedActivities) => 
					Mav("home/activities",
						"activities" -> pagedActivities,
						"async" -> true).noLayout
				case _ => Mav("home/empty").noLayout
			}
		} catch {
			case e: IllegalStateException => {
				Mav("home/activities",
				"expired" -> true).noLayout
			}
		}
	}
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