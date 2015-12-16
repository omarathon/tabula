package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands.{SelfValidating, Appliable}
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridSelectCourseCommand
import uk.ac.warwick.tabula.data.model.{StudentCourseYearDetails, Department}
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.services.{AutowiringMaintenanceModeServiceComponent, AutowiringModuleAndDepartmentServiceComponent, AutowiringUserSettingsServiceComponent}
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.{Mav, Routes}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridSelectCourseController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent {

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	validatesSelf[SelfValidating]

	@ModelAttribute("selectCourseCommand")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear))

	@RequestMapping(method = Array(GET))
	def get(
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		Mav("exams/grids/generate/selectCourse")
			.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.departmentAcademicYear(department, year)): _*)
	}

	@RequestMapping(method = Array(POST))
	def selectCourse(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: Appliable[Seq[StudentCourseYearDetails]],
		errors: Errors,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			get(department, academicYear)
		} else {
			val students = selectCourseCommand.apply()
			// TODO: Create job to re-import selected students then render view to choose columns
			Mav("exams/grids/generate/gridOption")
				.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
				.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.departmentAcademicYear(department, year)): _*)
		}

	}

}
