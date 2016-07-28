package uk.ac.warwick.tabula.web.controllers.attendance

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.attendance.{HomeCommand, HomeInformation}
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

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
class AttendanceHomeController extends AttendanceController
	with AcademicYearScopedController with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

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
						retrieveActiveAcademicYear(None).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
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