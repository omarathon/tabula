package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringCourseAndRouteServiceComponent, CourseAndRouteServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.JavaImports._

object GenerateExamGridSelectCourseCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateExamGridSelectCourseCommandInternal(department, academicYear)
			with AutowiringCourseAndRouteServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with ComposableCommand[Seq[ExamGridEntity]]
			with GenerateExamGridSelectCourseValidation
			with GenerateExamGridSelectCoursePermissions
			with GenerateExamGridSelectCourseCommandState
			with GenerateExamGridSelectCourseCommandRequest
			with ReadOnly with Unaudited
}

class GenerateExamGridSelectCourseCommandInternal(val department: Department, val academicYear: AcademicYear) 
	extends CommandInternal[Seq[ExamGridEntity]] {

	self: StudentCourseYearDetailsDaoComponent with GenerateExamGridSelectCourseCommandRequest =>

	override def applyInternal() = {
		studentCourseYearDetailsDao.findByCourseRouteYear(
			academicYear,
			course,
			route,
			yearOfStudy,
			eagerLoad = true,
			disableFreshFilter = true
		).sortBy(_.studentCourseDetails.scjCode).map(scyd => scyd.studentCourseDetails.student.toExamGridEntity(yearOfStudy))
	}

}

trait GenerateExamGridSelectCourseValidation extends SelfValidating {

	self: GenerateExamGridSelectCourseCommandState with GenerateExamGridSelectCourseCommandRequest =>

	override def validate(errors: Errors) = {
		if (course == null) {
			errors.reject("examGrid.course.empty")
		} else if (!courses.contains(course)) {
			errors.reject("examGrid.course.invalid")
		}
		if (route == null) {
			errors.reject("examGrid.route.empty")
		} else if (!routes.contains(route)) {
			errors.reject("examGrid.route.invalid")
		}
		if (yearOfStudy == null) {
			errors.reject("examGrid.yearOfStudy.empty")
		} else if (!yearsOfStudy.contains(yearOfStudy)) {
			errors.reject("examGrid.yearOfStudy.invalid", Array(FilterStudentsOrRelationships.MaxYearsOfStudy.toString), "")
		}
	}
}

trait GenerateExamGridSelectCoursePermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridSelectCourseCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridSelectCourseCommandState {
	
	self: CourseAndRouteServiceComponent =>
	
	def department: Department
	def academicYear: AcademicYear

	lazy val courses = department.descendants.flatMap(d => courseAndRouteService.findCoursesInDepartment(d)).sortBy(_.code)
	lazy val routes = department.descendants.flatMap(d => courseAndRouteService.findRoutesInDepartment(d)).sortBy(_.code)
	lazy val yearsOfStudy = 1 to FilterStudentsOrRelationships.MaxYearsOfStudy
}

trait GenerateExamGridSelectCourseCommandRequest {
	var course: Course = _
	var route: Route = _
	var yearOfStudy: JInteger = _
}
