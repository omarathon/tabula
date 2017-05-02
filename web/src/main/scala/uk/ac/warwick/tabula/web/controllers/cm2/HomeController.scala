package uk.ac.warwick.tabula.web.controllers.cm2

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand
import uk.ac.warwick.tabula.commands.cm2.CourseworkHomepageCommand.Command
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

abstract class AbstractHomeController
	extends CourseworkController
		with AcademicYearScopedController
		with AutowiringUserSettingsServiceComponent
		with AutowiringMaintenanceModeServiceComponent {

	hideDeletedItems

	@ModelAttribute("command")
	def command(@ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): Command = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		CourseworkHomepageCommand(academicYear, user)
	}

	@RequestMapping
	def home(@ModelAttribute("command") command: Command, user: CurrentUser) = {
		val info = command.apply()

		Mav(s"$urlPrefix/home/view",
			"academicYear" -> command.academicYear,
			"homeDepartment" -> info.homeDepartment,
			"studentInformation" -> info.studentInformation,
			"markerInformation" -> info.markerInformation,
			"adminInformation" -> info.adminInformation
		).secondCrumbs(academicYearBreadcrumbs(command.academicYear)(Routes.homeForYear): _*)
	}

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}"))
class HomeController extends AbstractHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/{academicYear:\\d{4}}"))
class HomeForYearController extends AbstractHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}
