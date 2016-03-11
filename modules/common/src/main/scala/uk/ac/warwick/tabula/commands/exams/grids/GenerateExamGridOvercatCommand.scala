package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Module, ModuleRegistration, StudentCourseYearDetails}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.helpers.LazyMaps
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

import scala.collection.JavaConverters._

object GenerateExamGridOvercatCommand {
	def overcatIdentifier(modules: Seq[ModuleRegistration]) = modules.map(_.module.code).mkString("-")

	def apply(department: Department, academicYear: AcademicYear, scyd: StudentCourseYearDetails, normalLoad: BigDecimal, user: CurrentUser) =
		new GenerateExamGridOvercatCommandInternal(department, academicYear, scyd, normalLoad, user)
			with ComposableCommand[Seq[Module]]
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringModuleRegistrationServiceComponent
			with PopulateGenerateExamGridOvercatCommand
			with GenerateExamGridOvercatValidation
			with GenerateExamGridOvercatDescription
			with GenerateExamGridOvercatPermissions
			with GenerateExamGridOvercatCommandState
			with GenerateExamGridOvercatCommandRequest
}


class GenerateExamGridOvercatCommandInternal(
	val department: Department,
	val academicYear: AcademicYear,
	val scyd: StudentCourseYearDetails,
	val normalLoad: BigDecimal,
	val user: CurrentUser
)	extends CommandInternal[Seq[Module]] {

	self: GenerateExamGridOvercatCommandRequest with StudentCourseYearDetailsDaoComponent =>

	override def applyInternal() = {
		val modules = chosenModuleSubset.get._2.map(_.module)
		scyd.overcattingModules = modules
		scyd.overcattingChosenBy = user.apparentUser
		scyd.overcattingChosenDate = DateTime.now
		scyd.overcattingMarkOverrides = overwrittenMarks
		studentCourseYearDetailsDao.saveOrUpdate(scyd)
		modules
	}

}

trait PopulateGenerateExamGridOvercatCommand extends PopulateOnForm {

	self: GenerateExamGridOvercatCommandRequest with GenerateExamGridOvercatCommandState =>

	def populate(): Unit = {
		overcatChoice = scyd.overcattingModules.map(overcattingModules =>
			GenerateExamGridOvercatCommand.overcatIdentifier(
				scyd.moduleRegistrations.filter(mr => overcattingModules.contains(mr.module))
			)
		).getOrElse("")
		scyd.overcattingMarkOverrides.foreach(overcattingMarkOverrides =>
			overcattingMarkOverrides.foreach{case(module, mark) => newModuleMarks.put(module, mark.toString) }
		)
	}
}

trait GenerateExamGridOvercatValidation extends SelfValidating {

	self: GenerateExamGridOvercatCommandRequest =>

	override def validate(errors: Errors) {
		if (chosenModuleSubset.isEmpty) {
			errors.reject("examGrid.overcatting.overcatChoice.invalid")
		}
	}

}

trait GenerateExamGridOvercatPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridOvercatCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridOvercatDescription extends Describable[Seq[Module]] {

	self: GenerateExamGridOvercatCommandState with GenerateExamGridOvercatCommandRequest =>

	override lazy val eventName = "GenerateExamGridOvercat"

	override def describe(d: Description) {
		d.studentIds(Seq(scyd.studentCourseDetails.student.universityId))
		d.properties(Map(
			"studentCourseYearDetails" -> scyd.id,
			"modules" -> chosenModuleSubset.map{case(_, modules) => modules.map(_.module.code)}.getOrElse(Seq()),
			"mark" -> chosenModuleSubset.map{case(mark, _) => mark.toString}.getOrElse("")
		))
	}
}

trait GenerateExamGridOvercatCommandState {

	self: GenerateExamGridOvercatCommandRequest with ModuleRegistrationServiceComponent =>

	def department: Department
	def academicYear: AcademicYear
	def scyd: StudentCourseYearDetails
	def normalLoad: BigDecimal
	def user: CurrentUser

}

trait GenerateExamGridOvercatCommandRequest {

	self: GenerateExamGridOvercatCommandState with ModuleRegistrationServiceComponent =>

	var overcatChoice: String = _

	def chosenModuleSubset: Option[(BigDecimal, Seq[ModuleRegistration])] =
		moduleRegistrationService.overcattedModuleSubsets(scyd.toGenerateExamGridEntity(), overwrittenMarks, normalLoad)
			.find{case(_, modules) => GenerateExamGridOvercatCommand.overcatIdentifier(modules) == overcatChoice.maybeText.getOrElse("")}

	var newModuleMarks: JMap[Module, String] = LazyMaps.create { module: Module => null: String }.asJava

	def overwrittenMarks: Map[Module, BigDecimal] = newModuleMarks.asScala.map{case(module, markString) =>
		module -> markString.maybeText.map(mark => BigDecimal.apply(mark))
	}.filter(_._2.isDefined).mapValues(_.get).toMap

}
