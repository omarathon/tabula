package uk.ac.warwick.tabula.web.controllers.cm2.admin

import org.joda.time.DateTime
import org.springframework.context.annotation.Profile
import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.cm2.web.Routes
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand
import uk.ac.warwick.tabula.commands.cm2.assignments.ListAssignmentsCommand._
import uk.ac.warwick.tabula.data.model.Module
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.cm2.CourseworkController
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

abstract class AbstractModuleHomeController
	extends CourseworkController
		with AcademicYearScopedController
		with AutowiringUserSettingsServiceComponent
		with AutowiringMaintenanceModeServiceComponent {

	@ModelAttribute("command")
	def command(@PathVariable module: Module, @ModelAttribute("activeAcademicYear") activeAcademicYear: Option[AcademicYear], user: CurrentUser): ModuleCommand = {
		val academicYear = activeAcademicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))

		ListAssignmentsCommand.module(module, academicYear, user)
	}

	@RequestMapping(params=Array("!ajax"), headers=Array("!X-Requested-With"))
	def home(@ModelAttribute("command") command: ModuleCommand, @PathVariable module: Module): Mav =
		Mav("cm2/admin/home/module", "moduleInfo" -> command.apply(), "academicYear" -> command.academicYear)
			.crumbs(Breadcrumbs.Department(module.adminDepartment, command.academicYear))
			.secondCrumbs(academicYearBreadcrumbs(command.academicYear)(Routes.admin.module(module, _)): _*)

	@RequestMapping
	def homeAjax(@ModelAttribute("command") command: ModuleCommand): Mav =
		Mav("cm2/admin/home/assignments", "moduleInfo" -> command.apply()).noLayout()

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/{module}"))
class ModuleHomeController extends AbstractModuleHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/{module}/{academicYear:\\d{4}}"))
class ModuleHomeForYearController extends AbstractModuleHomeController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(academicYear))

}

@Profile(Array("cm2Enabled"))
@Controller
@RequestMapping(Array("/${cm2.prefix}/admin/module/{module}", "/${cm2.prefix}/admin/module/{module}/**"))
class ModuleHomeRedirectController extends CourseworkController
	with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	@RequestMapping
	def redirect(@PathVariable module: Module) =
		Redirect(Routes.admin.module(mandatory(module), retrieveActiveAcademicYear(None).getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))))

}
