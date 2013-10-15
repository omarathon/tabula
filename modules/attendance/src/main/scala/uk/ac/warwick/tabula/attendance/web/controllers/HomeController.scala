package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.CurrentUser
import uk.ac.warwick.tabula.attendance.commands.{HomeCommandState, HomeCommand}
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.Routes

/**
 * Displays the Attendance home screen.
 * Redirects to the the appropriate page if only one of the following is true:
 * * The user has a profile
 * * The user has view/record permissions on a single department
 * * The user has manage permissions on a single department
 * Otherwise they are shown the home page
 */
@Controller
@RequestMapping(Array("/"))
class HomeController extends AttendanceController {

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[Unit] with HomeCommandState) = {
		cmd.apply()
		val hasAnyRelationships = cmd.relationshipTypesMap.exists{ case (_, b) => b}
		if (cmd.hasProfile && cmd.managePermissions.size == 0 && cmd.viewPermissions.size == 0 && !hasAnyRelationships)
			Redirect(Routes.profile())
		else if (!cmd.hasProfile && cmd.managePermissions.size == 0 && cmd.viewPermissions.size == 1 && !hasAnyRelationships)
			Redirect(Routes.department.view(cmd.viewPermissions.head))
		else if (!cmd.hasProfile && cmd.managePermissions.size == 1 && cmd.viewPermissions.size == 0 && !hasAnyRelationships)
			Redirect(Routes.department.manage(cmd.managePermissions.head))
		else
			Mav("home/home",
				"relationshipTypesMapById" -> cmd.relationshipTypesMap.map { case (k, v) => (k.id, v) },
				"hasAnyRelationships" -> hasAnyRelationships
			)
	}

}