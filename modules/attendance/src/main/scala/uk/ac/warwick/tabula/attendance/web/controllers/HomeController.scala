package uk.ac.warwick.tabula.attendance.web.controllers

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.{Features, CurrentUser}
import uk.ac.warwick.tabula.attendance.commands.HomeCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.commands.HomeInformation
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.data.model.Department

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

	@Autowired var features: Features = _

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation]) = {
		val info = cmd.apply()

		val hasAnyRelationships = info.relationshipTypesMap.exists{ case (_, b) => b}

		if (info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.isEmpty && !hasAnyRelationships)
			Redirect(Routes.Profile.home)
			else if (!info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.size == 1 && !hasAnyRelationships) {
					Redirect(getViewDepartmentUrl(info.viewPermissions.head))
			} else if (!info.hasProfile && info.managePermissions.size == 1 && info.viewPermissions.isEmpty && !hasAnyRelationships) {
					Redirect(getManageDepartmentUrl(info.managePermissions.head))
			} else {
				Mav("home",
					"hasProfile" -> info.hasProfile,
					"relationshipTypesMap" -> info.relationshipTypesMap,
					"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
					"hasAnyRelationships" -> hasAnyRelationships,
					"viewPermissions" -> info.viewPermissions,
					"managePermissions" -> info.managePermissions
				)
		}
	}

	def getViewDepartmentUrl(department:Department) = {
		if (features.attendanceMonitoringVersion2) Routes.View.department(department)
		else Routes.old.department.view(department)
	}

	def getManageDepartmentUrl(department:Department) = {
		if (features.attendanceMonitoringVersion2) Routes.Manage.department(department)
		else Routes.old.department.manage(department)
	}

}