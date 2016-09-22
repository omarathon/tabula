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

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest

	case class Result(
		entities: Seq[ExamGridEntity],
		updatedEntities: Map[ExamGridEntity, (BigDecimal, Seq[ModuleRegistration])]
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
		val updatedEntities = filteredEntities.map { entity =>
			val scyd = entity.years
				.getOrElse(selectCourseCommand.yearOfStudy, throw new IllegalArgumentException(s"Could not find entity year for year ${selectCourseCommand.yearOfStudy}"))
				.studentCourseYearDetails.get

			// Set the choice to be the first one (the one with the highest mark)
			val chosenModuleSubset = overcatSubsets(entity).head

			// Save the overcat choice
			scyd.overcattingModules = chosenModuleSubset._2.map(_.module)
			scyd.overcattingChosenBy = user.apparentUser
			scyd.overcattingChosenDate = DateTime.now
			studentCourseYearDetailsDao.saveOrUpdate(scyd)
			entity -> chosenModuleSubset
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
		d.property("entities", result.updatedEntities.map{ case (entity, (mark, modules)) =>
			Map(
				"universityId" -> entity.universityId,
				"studentCourseYearDetails" -> entity.years(selectCourseCommand.yearOfStudy).studentCourseYearDetails.get.id,
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
	var yearsToShow: String = "current"

	def fetchEntities = selectCourseCommand.apply()
	lazy val entities = yearsToShow match {
		case "all" => fetchEntities
		case _ => fetchEntities.map(entity => entity.copy(years = Map(entity.years.keys.max -> entity.years(entity.years.keys.max))))
	}
	lazy val normalLoadOption = upstreamRouteRuleService.findNormalLoad(
		selectCourseCommand.route,
		academicYear,
		selectCourseCommand.yearOfStudy
	)
	lazy val normalLoad = normalLoadOption.getOrElse(selectCourseCommand.route.degreeType.normalCATSLoad)
	lazy val routeRules = upstreamRouteRuleService.list(
		selectCourseCommand.route,
		academicYear,
		selectCourseCommand.yearOfStudy
	)

	lazy val overcatSubsets: Map[ExamGridEntity, Seq[(BigDecimal, Seq[ModuleRegistration])]] =
		entities.filter(_.years.get(selectCourseCommand.yearOfStudy).isDefined).map(entity => entity ->
			moduleRegistrationService.overcattedModuleSubsets(
				entity.years(selectCourseCommand.yearOfStudy),
				entity.years(selectCourseCommand.yearOfStudy).markOverrides.getOrElse(Map()),
				normalLoad,
				routeRules
			)
		).toMap

	lazy val filteredEntities: Seq[ExamGridEntity] =
		entities.filter(entity =>
			// Filter entities to those that have a entity year for the give academic year
			// and have more than one some overcat subset
			overcatSubsets.exists { case (overcatEntity, subsets) => overcatEntity == entity && subsets.size > 1 }
		).filter(entity => entity.years(selectCourseCommand.yearOfStudy).overcattingModules match {
			// And either their current overcat choice is empty...
			case None => true
			// Or the highest mark is now a different set of modules (in case the rules have changed)
			case Some(overcattingModules) =>
				val highestSubset = overcatSubsets(entity).head._2
				highestSubset.map(_.module).size != overcattingModules.size ||
					highestSubset.exists(mr => !overcattingModules.contains(mr.module))
		})
}
