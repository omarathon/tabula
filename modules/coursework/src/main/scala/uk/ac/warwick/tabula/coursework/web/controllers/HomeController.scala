package uk.ac.warwick.tabula.coursework.web.controllers

import org.springframework.stereotype.Controller
import uk.ac.warwick.tabula.commands.coursework.assignments.{CourseworkHomepageActivityPageletCommand, CourseworkHomepageCommand, StudentCourseworkFullScreenCommand}
import CourseworkHomepageCommand.CourseworkHomepageInformation
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.services.ActivityService.PagedActivities
import org.springframework.web.bind.annotation._
import uk.ac.warwick.tabula.commands.Appliable
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
					"ownedModuleDepartments" -> info.ownedModules.map { _.adminDepartment },
					"activities" -> info.activities,
					"ajax" -> ajax)
			case _ => Mav("home/view")
		}
}

@Controller class HomeActivitiesPageletController extends CourseworkController {

	hideDeletedItems

	@ModelAttribute("command") def command(
		user: CurrentUser,
		@PathVariable("doc") doc: Int,
		@PathVariable("field") field: Long,
		@PathVariable("token") token: Long) =
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