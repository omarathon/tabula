package uk.ac.warwick.tabula.commands.exams.grids

import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.commands.exams.grids.GenerateExamGridCheckAndApplyOvercatCommand.{Result, SelectCourseCommand}
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model.{Department, ModuleRegistration, UpstreamRouteRuleLookup}
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.exams.grids._
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

object GenerateExamGridCheckAndApplyOvercatCommand {

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest

	case class Result(
		entities: Seq[ExamGridEntity],
		updatedEntities:  Map[ExamGridEntity, Map[YearOfStudy, (BigDecimal, Seq[ModuleRegistration])]]
	)

	def apply(department: Department, academicYear: AcademicYear, user: CurrentUser) =
		new GenerateExamGridCheckAndApplyOvercatCommandInternal(department, academicYear, user)
			with ComposableCommand[Result]
			with AutowiringUpstreamRouteRuleServiceComponent
			with AutowiringModuleRegistrationServiceComponent
			with AutowiringStudentCourseYearDetailsDaoComponent
			with AutowiringNormalCATSLoadServiceComponent
			with GenerateExamGridCheckAndApplyOvercatValidation
			with GenerateExamGridCheckAndApplyOvercatDescription
			with GenerateExamGridCheckAndApplyOvercatPermissions
			with GenerateExamGridCheckAndApplyOvercatCommandState
}


class GenerateExamGridCheckAndApplyOvercatCommandInternal(val department: Department, val academicYear: AcademicYear, user: CurrentUser)
	extends CommandInternal[GenerateExamGridCheckAndApplyOvercatCommand.Result] {

	self: ModuleRegistrationServiceComponent with GenerateExamGridCheckAndApplyOvercatCommandState
	with StudentCourseYearDetailsDaoComponent =>

	override def applyInternal(): Result = {
		val updatedEntities = filteredEntities.map { entity =>
			val years = entity.validYears
				.filter{case (year, _) => overcatSubsets(entity).get(year).isDefined && overcatSubsets(entity)(year).nonEmpty}
				.map{ case( year, entityYear) =>
					val scyd = entityYear.studentCourseYearDetails.get
					val chosenModuleSubset = overcatSubsets(entity)(year).head

					// Save the overcat choice
					scyd.overcattingModules = chosenModuleSubset._2.map(_.module)
					scyd.overcattingChosenBy = user.apparentUser
					scyd.overcattingChosenDate = DateTime.now
					studentCourseYearDetailsDao.saveOrUpdate(scyd)
					year -> chosenModuleSubset
				}

			entity -> years
		}

		// Re-fetch the entities to account for the newly chosen subset
		GenerateExamGridCheckAndApplyOvercatCommand.Result(fetchEntities, updatedEntities.toMap)
	}

}

trait GenerateExamGridCheckAndApplyOvercatValidation extends SelfValidating {

	self: GenerateExamGridCheckAndApplyOvercatCommandState =>

	override def validate(errors: Errors) {
		if (filteredEntities.isEmpty){
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
		d.property("entities", result.updatedEntities.map{ case (entity, years) =>
			Map(
				"universityId" -> entity.universityId,
				"years" -> years.map{ case (year, (mark, modules)) => Map(
					"studentCourseYearDetails" -> entity.years(year).get.studentCourseYearDetails.get.id,
					"modules" -> modules.map(_.module.code),
					"mark" -> mark.toString
				)}
			)
		})
	}
}

trait GenerateExamGridCheckAndApplyOvercatCommandState {

	self: UpstreamRouteRuleServiceComponent with ModuleRegistrationServiceComponent
		with NormalCATSLoadServiceComponent =>

	def department: Department
	def academicYear: AcademicYear

	var selectCourseCommand: SelectCourseCommand = _
	var yearsToShow: String = "current"

	def fetchEntities: Seq[ExamGridEntity] = selectCourseCommand.apply()
	lazy val entities: Seq[ExamGridEntity] = yearsToShow match {
		case "all" => fetchEntities
		case _ => fetchEntities.map(entity => entity.copy(years = Map(entity.years.keys.max -> entity.years(entity.years.keys.max))))
	}
	lazy val normalLoadLookup: NormalLoadLookup = new NormalLoadLookup(academicYear, selectCourseCommand.yearOfStudy, normalCATSLoadService)
	lazy val routeRulesLookup: UpstreamRouteRuleLookup = new UpstreamRouteRuleLookup(academicYear, upstreamRouteRuleService)

	lazy val overcatSubsets: Map[ExamGridEntity, Map[YearOfStudy, Seq[(BigDecimal, Seq[ModuleRegistration])]]] =
		entities.map(entity => {
			val subsets = entity.validYears
				.filter{case (year, entityYear) => routeRulesLookup(entityYear.route, entityYear.level).nonEmpty}
		  	.mapValues(entityYear => moduleRegistrationService.overcattedModuleSubsets(
					entityYear,
					entityYear.markOverrides.getOrElse(Map()),
					normalLoadLookup(entityYear.route),
					routeRulesLookup(entityYear.route, entityYear.level)
				))
				.map{case (year, overcattedModuleSubsets) =>
					if(overcattedModuleSubsets.size > 1) year -> overcattedModuleSubsets
					else year -> Seq()
				}
			entity -> subsets
		}).toMap

	lazy val filteredEntities: Seq[ExamGridEntity] =
		entities.filter(entity =>
			// Filter entities to those that have some route rules applied (done in overcatSubsets)
			// and have more than one overcat subset for at least one academic year
			overcatSubsets.exists{ case (overcatEntity, subsets) => overcatEntity == entity && subsets.values.exists(_.size > 1) }
		).filter(entity =>
			entity.validYears.exists{ case (year, entityYear)=> entityYear.overcattingModules match {
				// And either their current overcat choice is empty...
				case None => true
				// Or the highest mark is now a different set of modules (in case the rules have changed)
				case Some(overcattingModules) =>
					val highestSubset = overcatSubsets(entity).get(year).flatMap(_.headOption.map{case (_, modules) => modules})
					highestSubset match {
						case Some(subset) => subset.map(_.module).size != overcattingModules.size || subset.exists(mr => !overcattingModules.contains(mr.module))
						case _ => false
					}
			}}
		)
}
