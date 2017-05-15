package uk.ac.warwick.tabula.web.controllers.coursework

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.commands.{Appliable, MemberOrUser}
import uk.ac.warwick.tabula.commands.coursework.assignments.CourseworkHomepageCommand.CourseworkHomepageInformation
import uk.ac.warwick.tabula.commands.coursework.assignments.{CourseworkHomepageActivityPageletCommand, CourseworkHomepageCommand}
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import uk.ac.warwick.tabula.services.AutowiringModuleAndDepartmentServiceComponent
import uk.ac.warwick.tabula.web.Mav

@Profile(Array("cm1Enabled")) @Controller class OldHomeController extends OldCourseworkController with AutowiringModuleAndDepartmentServiceComponent {

	hideDeletedItems

	@ModelAttribute("command") def command(user: CurrentUser) = CourseworkHomepageCommand(user)

	@RequestMapping(Array("/${cm1.prefix}")) def home(@ModelAttribute("command") cmd: Appliable[Option[CourseworkHomepageInformation]], user: CurrentUser): Mav =
		cmd.apply() match {
			case Some(info) =>
				Mav("coursework/home/view",
					"student" -> MemberOrUser(user.profile, user.apparentUser),
					"enrolledAssignments" -> info.enrolledAssignments,
					"historicAssignments" -> info.historicAssignments,
					"assignmentsForMarking" -> info.assignmentsForMarking,
					"ownedDepartments" -> info.ownedDepartments,
					"ownedModule" -> info.ownedModules,
					"ownedModuleDepartments" -> info.ownedModules.map { _.adminDepartment },
					"ajax" -> ajax,
					"userHomeDepartment" -> moduleAndDepartmentService.getDepartmentByCode(user.departmentCode)
				)
			case _ => Mav("coursework/home/view")
		}
}

@Profile(Array("cm1Enabled")) @Controller class OldHomeActivitiesPageletController extends OldCourseworkController {

	hideDeletedItems

	@ModelAttribute("command") def command(
		user: CurrentUser,
		@PathVariable lastUpdatedDate: Long) =
			CourseworkHomepageActivityPageletCommand(user, new DateTime(lastUpdatedDate))

	@RequestMapping(Array("/${cm1.prefix}/api/activity/pagelet/{lastUpdatedDate}"))
	def pagelet(@ModelAttribute("command") cmd: Appliable[Option[PagedActivities]]): Mav = {
		try {
			cmd.apply() match {
				case Some(pagedActivities) =>
					Mav("coursework/home/activities",
						"activities" -> pagedActivities,
						"async" -> true).noLayout()
				case _ => Mav("coursework/home/empty").noLayout()
			}
		} catch {
			case e: IllegalStateException =>
				Mav("coursework/home/activities",
				"expired" -> true).noLayout()
		}
	}
}