package uk.ac.warwick.tabula.web.controllers.attendance

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{RequestParam, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, Features, CurrentUser}
import uk.ac.warwick.tabula.commands.attendance.HomeCommand
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.attendance.HomeInformation
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
@RequestMapping(Array("/attendance"))
class AttendanceHomeController extends AttendanceController {

	@Autowired var features: Features = _

	@ModelAttribute("command")
	def createCommand(user: CurrentUser) = HomeCommand(user)

	@RequestMapping
	def home(@ModelAttribute("command") cmd: Appliable[HomeInformation], @RequestParam(value = "academicYear", required = false) academicYearOverride: AcademicYear) = {
		val info = cmd.apply()

		val hasAnyRelationships = info.relationshipTypesMap.exists{ case (_, b) => b}

		if (info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.isEmpty && !hasAnyRelationships)
			Redirect(Routes.Profile.home)
			else if (!info.hasProfile && info.managePermissions.isEmpty && info.viewPermissions.size == 1 && !hasAnyRelationships) {
					Redirect(Routes.View.department(info.viewPermissions.head))
			} else if (!info.hasProfile && info.managePermissions.size == 1 && info.viewPermissions.isEmpty && !hasAnyRelationships) {
					Redirect(Routes.Manage.department(info.managePermissions.head))
			} else {
				val academicYear = Option(academicYearOverride) match {
					case Some(year) =>
						year
					case _ =>
						if (features.attendanceMonitoringAcademicYear2015)
							AcademicYear(2015)
						else if (features.attendanceMonitoringAcademicYear2014)
							AcademicYear(2014)
						else
							AcademicYear(2013)
				}
				Mav("attendance/home",
					"hasProfile" -> info.hasProfile,
					"relationshipTypesMap" -> info.relationshipTypesMap,
					"relationshipTypesMapById" -> info.relationshipTypesMap.map { case (k, v) => (k.id, v) },
					"hasAnyRelationships" -> hasAnyRelationships,
					"viewPermissions" -> info.viewPermissions,
					"managePermissions" -> info.managePermissions,
					"academicYear" -> academicYear
				)
		}
	}

}