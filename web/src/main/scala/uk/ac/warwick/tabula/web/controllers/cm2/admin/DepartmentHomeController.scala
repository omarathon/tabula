package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand._
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.web.Mav

abstract class AbstractDepartmentHomeController
	extends CourseworkController
		with AcademicYearScopedController
		with AutowiringUserSettingsServiceComponent
		with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): Command = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		ListAssignmentsCommand(department, academicYear, user)
	}

	@RequestMapping
	def home(@ModelAttribute("command") command: Command, user: CurrentUser): Mav = {
		val info = command.apply()

		Mav(s"$urlPrefix/admin/home/view",
			"info" -> info,
			"academicYear" -> command.academicYear
		).secondCrumbs(academicYearBreadcrumbs(command.academicYear)(Routes.homeForYear): _*)
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}"))
class DepartmentHomeController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/department/{department}/{academicYear:\\d{4}}"))
class DepartmentHomeForYearController extends AbstractDepartmentHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}