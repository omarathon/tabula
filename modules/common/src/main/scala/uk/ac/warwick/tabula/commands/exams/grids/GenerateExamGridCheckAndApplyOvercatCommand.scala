package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridCheckAndApplyOvercatCommand.SelectCourseCommand
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.data.model.{ModuleRegistration, Department}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object GenerateExamGridCheckAndApplyOvercatCommand {

	type SelectCourseCommand = Appliable[Seq[GenerateExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest

	case class Result(
		entities: Seq[GenerateExamGridEntity],
		updatedEntities: Map[GenerateExamGridEntity, (BigDecimal, Seq[ModuleRegistration])]
	)

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new GenerateExamGridCheckAndApplyOvercatCommandInternal(department, academicYear, user)
			with ComposableCommand[Result]
			with AutowiringUpstreamRouteRuleServiceComponent
			with AutowiringModuleRegistrationServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with GenerateExamGridCheckAndApplyOvercatValidation
			with GenerateExamGridCheckAndApplyOvercatDescription
			with GenerateExamGridCheckAndApplyOvercatPermissions
			with GenerateExamGridCheckAndApplyOvercatCommandState
}


class GenerateExamGridCheckAndApplyOvercatCommandInternal(val department: Department, val academicYear: AcademicYear, user: CurrentUser)
	extends CommandInternal[GenerateExamGridCheckAndApplyOvercatCommand.Result] {

	self: ModuleRegistrationServiceComponent with GenerateExamGridCheckAndApplyOvercatCommandState
	with StudentCourseYearDetailsDaoComponent =>

	override def applyInternal() = {
		val updatedEntities = filteredEntities.map{ case(entity, subsetList) =>
			val scyd = entity.studentCourseYearDetails.get
			// Set the choice to be the first one (the one with the highest mark)
			val chosenModuleSubset = subsetList.head
			// Save the overcat choice
			scyd.overcattingModules = chosenModuleSubset._2.map(_.module)
			scyd.overcattingChosenBy = user.apparentUser
			scyd.overcattingChosenDate = DateTime.now
			studentCourseYearDetailsDao.saveOrUpdate(scyd)
			entity -> subsetList.head
		}
		// Re-fetch the entities to account for the newly chosen subset
		GenerateExamGridCheckAndApplyOvercatCommand.Result(fetchEntities, updatedEntities.toMap)
	}

}

trait GenerateExamGridCheckAndApplyOvercatValidation extends SelfValidating {

	self: GenerateExamGridCheckAndApplyOvercatCommandState =>

	override def validate(errors: Errors) {
		if (routeRules.isEmpty) {
			errors.reject("", "No route rules found")
		} else if (filteredEntities.isEmpty){
			errors.reject("", "No changes to apply")
		}
	}

}

trait GenerateExamGridCheckAndApplyOvercatPermissions extends RequiresPermissionsChecking with PermissionsCheckingMethods {

	self: GenerateExamGridCheckAndApplyOvercatCommandState =>

	override def permissionsCheck(p: PermissionsChecking) {
		p.PermissionCheck(Permissions.Department.ExamGrids, department)
	}

}

trait GenerateExamGridCheckAndApplyOvercatDescription extends Describable[GenerateExamGridCheckAndApplyOvercatCommand.Result] {

	self: GenerateExamGridCheckAndApplyOvercatCommandState =>

	override lazy val eventName = "GenerateExamGridCheckAndApplyOvercat"

	override def describe(d: Description) {
		d.department(department).property("academicYear", academicYear.toString)
	}

	override def describeResult(d: Description, result: GenerateExamGridCheckAndApplyOvercatCommand.Result): Unit = {
		d.property("entities", result.updatedEntities.map{case(entity, (mark, modules)) =>
			Map(
				"universityId" -> entity.universityId,
				"studentCourseYearDetails" -> entity.studentCourseYearDetails.get.id,
				"modules" -> modules.map(_.module.code),
				"mark" -> mark.toString
			)
		})
	}
}

trait GenerateExamGridCheckAndApplyOvercatCommandState {

	self: UpstreamRouteRuleServiceComponent with ModuleRegistrationServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	var selectCourseCommand: SelectCourseCommand = _

	def fetchEntities = selectCourseCommand.apply().sortBy(_.studentCourseYearDetails.get.studentCourseDetails.scjCode)
	lazy val entities = fetchEntities
	lazy val normalLoadOption = upstreamRouteRuleService.findNormalLoad(
		selectCourseCommand.route,
		academicYear,
		selectCourseCommand.yearOfStudy
	)
	lazy val normalLoad = normalLoadOption.getOrElse(ModuleRegistrationService.DefaultNormalLoad)
	lazy val routeRules = upstreamRouteRuleService.list(
		selectCourseCommand.route,
		academicYear,
		selectCourseCommand.yearOfStudy
	)

	lazy val filteredEntities = entities.filter(entity =>
		// For any student that has overcatted...
		entity.cats > ModuleRegistrationService.DefaultNormalLoad
	).map(entity => entity ->
		// Build the subsets...
		moduleRegistrationService.overcattedModuleSubsets(entity, entity.markOverrides.getOrElse(Map()), normalLoad, routeRules)
	).filter(
		// Only keep entities where a valid subset exists...
		_._2.nonEmpty
	).filter{ case(entity, subsets) => entity.overcattingModules match {
		// And either their current overcat choice is empty...
		case None => true
		// Or the highest mark is now a different set of modules (in case the rules have changed)
		case Some(overcattingModules) =>
			subsets.head._2.map(_.module).size != overcattingModules.size ||
				subsets.head._2.exists(mr => !overcattingModules.contains(mr.module))
	}}
}
