package uk.ac.warwick.tabula.commands.exams.grids

import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleAndDepartmentServiceComponent, ModuleAndDepartmentServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object GenerateExamGridSetCoreRequiredModulesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateExamGridSetCoreRequiredModulesCommandInternal(department, academicYear)
			with AutowiringModuleAndDepartmentServiceComponent
			with ComposableCommand[Seq[Module]]
			with GenerateExamGridSetCoreRequiredModulesDescription
			with GenerateExamGridSetCoreRequiredModulesPermissions
			with GenerateExamGridSetCoreRequiredModulesCommandState
			with GenerateExamGridSetCoreRequiredModulesCommandRequest
}


class GenerateExamGridSetCoreRequiredModulesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[Module]] {

	self: GenerateExamGridSetCoreRequiredModulesCommandRequest with ModuleAndDepartmentServiceComponent =>

	override def applyInternal() = {
		department.setCoreRequiredModules(academicYear, course, route, yearOfStudy.toInt, modules.asScala.toSeq)
		moduleAndDepartmentService.save(department)
		modules.asScala.toSeq
	}

}

trait GenerateExamGridSetCoreRequiredModulesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridSetCoreRequiredModulesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridSetCoreRequiredModulesDescription extends Describable[Seq[Module]] {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest =>

	override lazy val eventName = "GenerateExamGridSetCoreRequiredModules"

	override def describe(d: Description) {
		d.department(department)
			.properties(
				"academicYear" -> academicYear.toString,
				"course" -> course.code,
				"route" -> route.code,
				"yearOfStudy" -> yearOfStudy,
				"modules" -> modules.asScala.map(_.code)
			)
	}
}

trait GenerateExamGridSetCoreRequiredModulesCommandState {
	def department: Department
	def academicYear: AcademicYear
}

trait GenerateExamGridSetCoreRequiredModulesCommandRequest extends GenerateExamGridSelectCourseCommandRequest {
	var modules: JSet[Module] = JHashSet()
}
