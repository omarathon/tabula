package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.manage.{AddTemplatePointsToSchemesCommandState, AddTemplatePointsToSchemesCommand}
import uk.ac.warwick.tabula.data.model.attendance.{AttendanceMonitoringPoint, AttendanceMonitoringScheme}
import org.springframework.beans.factory.annotation.Autowired
import uk.ac.warwick.tabula.services.AttendanceMonitoringService
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.attendance.web.Routes
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.attendance.commands.GroupsPoints
import org.springframework.validation.{Errors, BindException}


@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/addpoints/template"))
class AddPointsToSchemesFromTemplateController extends AttendanceController {

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		AddTemplatePointsToSchemesCommand(mandatory(department), mandatory(academicYear))
	}

	@RequestMapping(method = Array(POST))
	def post(
		@ModelAttribute("command") cmd: AddTemplatePointsToSchemesCommandState,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		Mav("manage/templates",
			"schemes" -> cmd.schemes,
			"templates" -> cmd.templateSchemeItems,
			"department" -> cmd.schemes.get(0).department,
			"academicYear" -> cmd.academicYear.startYear.toString,
			"returnTo" -> getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear))
			).crumbs(
			Breadcrumbs.Manage.Home,
			Breadcrumbs.Manage.Department(department),
			Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
	}

	@RequestMapping(method = Array(POST), params = Array("templateScheme"))
	def submit(
		@ModelAttribute("command") cmd: Appliable[Seq[AttendanceMonitoringPoint]] with AddTemplatePointsToSchemesCommandState with SelfValidating,
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {

		cmd.validate(errors)

		if(errors.hasErrors){
			Mav("manage/templates",
				"schemes" -> cmd.schemes,
				"templates" -> cmd.templateSchemeItems,
				"department" -> cmd.schemes.get(0).department,
				"academicYear" -> cmd.academicYear.startYear.toString,
				"returnTo" -> getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear)),
				"errors" -> errors).crumbs(
					Breadcrumbs.Manage.Home,
					Breadcrumbs.Manage.Department(department),
					Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
				)
		} else {
			val points = cmd.apply()
			Redirect(getReturnTo(Routes.Manage.addPointsToExistingSchemes(department, academicYear)),
				"points" -> points.size.toString,
				"schemes" -> points.map(_.scheme.id).mkString(",")
			)
		}

	}

}
