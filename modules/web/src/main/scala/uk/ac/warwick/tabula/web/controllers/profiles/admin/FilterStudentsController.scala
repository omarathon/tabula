package uk.ac.warwick.tabula.web.controllers.profiles.admin

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.Appliable
import uk.ac.warwick.tabula.commands.profiles.{FilterStudentsCommand, FilterStudentsResults}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.profiles.web.Routes
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.BreadCrumb
import uk.ac.warwick.tabula.web.controllers.AcademicYearScopedController
import uk.ac.warwick.tabula.web.controllers.profiles.{ProfileBreadcrumbs, ProfilesController}

abstract class AbstractFilterStudentsAcademicYearController extends ProfilesController
	with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent
	with AutowiringMaintenanceModeServiceComponent {

	protected def breadcrumbs(academicYear: AcademicYear, department: Department): Seq[BreadCrumb] =
		Seq(
			ProfileBreadcrumbs.DepartmentalStudentProfiles.Students(department, academicYear)
		)

	@ModelAttribute("filterStudentsCommand")
	def command(@PathVariable department: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) =
		FilterStudentsCommand(mandatory(department), academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now)))

	@RequestMapping
	def filter(@ModelAttribute("filterStudentsCommand") cmd: Appliable[FilterStudentsResults], errors: Errors, @PathVariable department: Department, @ModelAttribute("activeAcademicYear") academicYear: Option[AcademicYear]) = {
		if (errors.hasErrors) {
			Mav("profiles/profile/filter/filter").noLayout()
		} else {
			val year = academicYear.getOrElse(AcademicYear.guessSITSAcademicYearByDate(DateTime.now))
			val results = cmd.apply()
			val modelMap = Map(
				"students" -> results.students,
				"totalResults" -> results.totalResults,
				"academicYear" -> year
			)
			if (ajax) Mav("profiles/profile/filter/results", modelMap).noLayout()
			else Mav("profiles/profile/filter/filter", modelMap).crumbs(breadcrumbs(year, department): _*)
				.secondCrumbs(academicYearBreadcrumbs(year)(year => Routes.Profile.students(department, year)): _*)
		}
	}
}

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/students/{academicYear}"))
class FilterStudentsAcademicYearController extends AbstractFilterStudentsAcademicYearController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] =
		retrieveActiveAcademicYear(Option(mandatory(academicYear)))

}

@Controller
@RequestMapping(value = Array("/profiles/department/{department}/students"))
class FilterStudentsController extends AbstractFilterStudentsAcademicYearController {

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear: Option[AcademicYear] =
		retrieveActiveAcademicYear(None)
}