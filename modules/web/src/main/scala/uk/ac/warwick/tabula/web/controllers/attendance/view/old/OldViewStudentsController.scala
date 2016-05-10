package uk.ac.warwick.tabula.web.controllers.attendance.view.old

import javax.validation.Valid

import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.{Appliable, SelfValidating}
import uk.ac.warwick.tabula.commands.attendance.view.old.{ViewStudentsCommand, ViewStudentsResults, ViewStudentsState}
import uk.ac.warwick.tabula.data.model.{Department, StudentMember}
import uk.ac.warwick.tabula.web.controllers.attendance.AttendanceController

@RequestMapping(Array("/attendance/view/{department}/2013/students"))
class OldViewStudentsController extends AttendanceController {

	validatesSelf[SelfValidating]

	@ModelAttribute("command")
	def command(@PathVariable department: Department) =
		ViewStudentsCommand(department, Option(AcademicYear(2013)), user)

	@RequestMapping
	def filter(
		@Valid @ModelAttribute("command") cmd: Appliable[ViewStudentsResults] with ViewStudentsState,
		errors: Errors,
		@RequestParam(value="updatedStudent", required = false) updatedStudent: StudentMember,
		@RequestParam(value="reports", required = false) reports: JInteger,
		@RequestParam(value="monitoringPeriod", required = false) monitoringPeriod: String
	) = {
		if (errors.hasErrors) {
			if (ajax)
				Mav("attendance/home/view_students_results").noLayout()
			else
				Mav("attendance/home/view_students_filter",
					"updatedStudent" -> updatedStudent,
					"reports" -> reports,
					"monitoringPeriod" -> monitoringPeriod
				).crumbs(Breadcrumbs.Old.ViewDepartment(cmd.department))
		} else {
			val results = cmd.apply()

			if (ajax)
				Mav("attendance/home/view_students_results",
					"students" -> results.students,
					"totalResults" -> results.totalResults,
					"necessaryTerms" -> results.students.flatMap{ data => data.pointsByTerm.keySet }.distinct
				).noLayout()
			else
				Mav("attendance/home/view_students_filter",
					"students" -> results.students,
					"totalResults" -> results.totalResults,
					"updatedStudent" -> updatedStudent,
					"reports" -> reports,
					"monitoringPeriod" -> monitoringPeriod,
					"necessaryTerms" -> results.students.flatMap{ data => data.pointsByTerm.keySet }.distinct
				).crumbs(Breadcrumbs.Old.ViewDepartment(cmd.department))
		}
	}

}