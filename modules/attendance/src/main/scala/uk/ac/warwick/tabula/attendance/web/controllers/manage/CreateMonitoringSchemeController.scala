package uk.ac.warwick.tabula.attendance.web.controllers.manage

import org.springframework.stereotype.Controller
import org.springframework.web.bind.annotation.{PathVariable, ModelAttribute, RequestMapping}
import uk.ac.warwick.tabula.attendance.web.controllers.AttendanceController
import uk.ac.warwick.tabula.data.model.Department
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.attendance.commands.manage.CreateMonitoringSchemeCommand
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.data.model.attendance.AttendanceMonitoringScheme
import javax.validation.Valid
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.attendance.web.Routes

@Controller
@RequestMapping(Array("/manage/{department}/{academicYear}/new"))
class CreateMonitoringSchemeController extends AttendanceController {

	final val createAndAddStudentsString = "createAndAddStudents"

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		CreateMonitoringSchemeCommand(department, academicYear)

	@RequestMapping(method = Array(GET, HEAD))
	def form(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) = {
		Mav("manage/new", "createAndAddStudentsString" -> createAndAddStudentsString)
			.crumbs(
				Breadcrumbs.Manage.Home,
				Breadcrumbs.Manage.Department(department),
				Breadcrumbs.Manage.DepartmentForYear(department, academicYear)
			)
	}

	@RequestMapping(method = Array(POST))
	def post(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			cmd.apply()
			Redirect(Routes.Manage.departmentForYear(department, academicYear))
		}
	}

	@RequestMapping(method = Array(POST), params = Array(createAndAddStudentsString))
	def postAndAddStudents(
		@Valid @ModelAttribute("command") cmd: Appliable[AttendanceMonitoringScheme],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	) = {
		if (errors.hasErrors) {
			form(department, academicYear)
		} else {
			val scheme = cmd.apply()
			Redirect(Routes.Manage.addStudentsToScheme(scheme))
		}
	}

}
