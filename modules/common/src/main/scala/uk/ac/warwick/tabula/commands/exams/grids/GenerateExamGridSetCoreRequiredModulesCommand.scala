package uk.ac.warwick.tabula.commands.exams.grids

import org.springframework.validation.Errors
import uk.ac.warwick.tabula.AcademicYear
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{CoreRequiredModule, Department, Module}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

import scala.collection.JavaConverters._

object GenerateExamGridSetCoreRequiredModulesCommand {
	def apply(department: Department, academicYear: AcademicYear) =
		new GenerateExamGridSetCoreRequiredModulesCommandInternal(department, academicYear)
			with AutowiringModuleAndDepartmentServiceComponent
			with AutowiringModuleRegistrationServiceComponent
			with ComposableCommand[Seq[CoreRequiredModule]]
			with PopulateGenerateExamGridSetCoreRequiredModulesCommand
			with GenerateExamGridSetCoreRequiredModulesCommandValidation
			with GenerateExamGridSetCoreRequiredModulesDescription
			with GenerateExamGridSetCoreRequiredModulesPermissions
			with GenerateExamGridSetCoreRequiredModulesCommandState
			with GenerateExamGridSetCoreRequiredModulesCommandRequest
			with GenerateExamGridSelectCourseCommandRequest
}

class GenerateExamGridSetCoreRequiredModulesCommandInternal(val department: Department, val academicYear: AcademicYear)
	extends CommandInternal[Seq[CoreRequiredModule]] {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
		with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent =>

	override def applyInternal(): Seq[CoreRequiredModule] = {
		val newModules = modules.asScala.map(module =>
			existingCoreRequiredModules.find(_.module == module).getOrElse(
				new CoreRequiredModule(route, academicYear, yearOfStudy, module)
			)
		).toSeq
		val modulesToRemove = existingCoreRequiredModules.filterNot(newModules.contains)
		modulesToRemove.foreach(moduleRegistrationService.delete)
		newModules.foreach(moduleRegistrationService.saveOrUpdate)
		newModules
	}

}

trait PopulateGenerateExamGridSetCoreRequiredModulesCommand extends PopulateOnForm {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest =>

	def populate(): Unit = {
		modules.addAll(existingCoreRequiredModules.map(_.module).asJava)
	}

}

trait GenerateExamGridSetCoreRequiredModulesCommandValidation extends SelfValidating {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest =>

	override def validate(errors: Errors): Unit = {
		val invalidModules = modules.asScala.filterNot(allModules.contains)
		if (invalidModules.nonEmpty) {
			errors.reject("examGrid.coreRequiredModulesInvalid", Array(invalidModules.map(_.code).mkString(", ")), "")
		}
	}

}

trait GenerateExamGridSetCoreRequiredModulesPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridSetCoreRequiredModulesCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridSetCoreRequiredModulesDescription extends Describable[Seq[CoreRequiredModule]] {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSetCoreRequiredModulesCommandRequest
		with GenerateExamGridSelectCourseCommandRequest =>

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

	self: ModuleAndDepartmentServiceComponent with GenerateExamGridSelectCourseCommandRequest with ModuleRegistrationServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	lazy val allModules: Seq[Module] = moduleAndDepartmentService.findByRouteYearAcademicYear(route, yearOfStudy, academicYear)
	lazy val existingCoreRequiredModules: Seq[CoreRequiredModule] = moduleRegistrationService.findCoreRequiredModules(route, academicYear, yearOfStudy)
}

trait GenerateExamGridSetCoreRequiredModulesCommandRequest {

	self: GenerateExamGridSetCoreRequiredModulesCommandState with GenerateExamGridSelectCourseCommandRequest =>

	var modules: JSet[Module] = JHashSet()
}
