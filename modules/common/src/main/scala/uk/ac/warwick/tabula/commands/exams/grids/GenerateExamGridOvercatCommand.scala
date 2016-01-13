package uk.ac.warwick.tabula.commands.exams.grids

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.joda.time.DateTime
import org.springframework.validation.Errors
import uk.ac.warwick.tabula.data.{AutowiringStudentCourseYearDetailsDaoComponent, StudentCourseYearDetailsDaoComponent}
import uk.ac.warwick.tabula.{CurrentUser, AcademicYear}
import uk.ac.warwick.tabula.commands._
import uk.ac.warwick.tabula.data.model.{Department, Module, ModuleRegistration, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.marking.{CurrentYearMarkColumnOption, OvercattedYearMarkColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.modules._
import uk.ac.warwick.tabula.exams.grids.columns.{HasExamGridColumnCategory, ExamGridColumn, ExamGridColumnOption}
import uk.ac.warwick.tabula.permissions.Permissions
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.system.permissions.{PermissionsChecking, PermissionsCheckingMethods, RequiresPermissionsChecking}

object GenerateExamGridOvercatCommand {
	def overcatIdentifier(modules: Seq[ModuleRegistration]) = modules.map(_.module.code).mkString("-")

	def apply(department: Department, academicYear: AcademicYear, scyd: StudentCourseYearDetails, user: CurrentUser) =
		new GenerateExamGridOvercatCommandInternal(department, academicYear, scyd, user)
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


class GenerateExamGridOvercatCommandInternal(val department: Department, val academicYear: AcademicYear, val scyd: StudentCourseYearDetails, val user: CurrentUser)
	extends CommandInternal[Seq[Module]] {

	self: GenerateExamGridOvercatCommandRequest with StudentCourseYearDetailsDaoComponent =>

	override def applyInternal() = {
		val modules = chosenModuleSubset.get._2.map(_.module)
		scyd.overcattingModules = modules
		scyd.overcattingChosenBy = user.apparentUser
		scyd.overcattingChosenDate = DateTime.now
		scyd.overcattingModules
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
	def user: CurrentUser

	private lazy val departmentCoreRequiredModules = department.getCoreRequiredModules(
		academicYear,
		scyd.studentCourseDetails.course,
		scyd.studentCourseDetails.route,
		scyd.yearOfStudy
	).getOrElse(Seq())

	protected lazy val overcattedMarks: Seq[(BigDecimal, Seq[ModuleRegistration])] =
		moduleRegistrationService.overcattedModuleSubsets(scyd.toGenerateExamGridEntity())

	protected lazy val overcattedEntities = overcattedMarks.map{case(mark, overcattedModules) => GenerateExamGridEntity(
		GenerateExamGridOvercatCommand.overcatIdentifier(overcattedModules),
		null,
		null,
		overcattedModules,
		scyd.normalCATLoad,
		Some(overcattedModules.map(_.module)),
		None
	)}

	lazy val previousYearsSCYDs: Seq[StudentCourseYearDetails] = scyd.studentCourseDetails.student.freshStudentCourseDetails.flatMap(_.freshStudentCourseYearDetails)
		.groupBy(_.academicYear)
		.filterKeys(academicYear > _)
		.mapValues(_.sortBy(s => (s.studentCourseDetails.scjCode, s.sceSequenceNumber)).reverse.head)
		.values.toSeq.sortBy(_.academicYear.startYear).reverse

	private lazy val allModulesEntities = overcattedMarks.map{case(mark, overcattedModules) => GenerateExamGridEntity(
		GenerateExamGridOvercatCommand.overcatIdentifier(overcattedModules),
		null,
		null,
		scyd.moduleRegistrations,
		scyd.normalCATLoad,
		Some(scyd.moduleRegistrations.map(_.module)),
		None
	)}

	lazy val optionsColumns: Seq[ExamGridColumn] = Seq(
		new ChooseOvercatColumnOption().getColumns(overcattedEntities, Option(overcatChoice).getOrElse("")),
		new OvercattedYearMarkColumnOption().getColumns(overcattedEntities),
		new CurrentYearMarkColumnOption().getColumns(allModulesEntities)
	).flatten

	lazy val optionsColumnsCategories: Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]] =
		optionsColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

	lazy val optionsColumnValues: Seq[Map[String, String]] = optionsColumns.map(_.render)

	lazy val columnsBySCYD: Map[StudentCourseYearDetails, Seq[ExamGridColumn]] = Map(
		scyd -> Seq(
			new CoreModulesColumnOption().getColumns(departmentCoreRequiredModules, overcattedEntities),
			new CoreRequiredModulesColumnOption().getColumns(departmentCoreRequiredModules, overcattedEntities),
			new CoreOptionalModulesColumnOption().getColumns(departmentCoreRequiredModules, overcattedEntities),
			new OptionalModulesColumnOption().getColumns(departmentCoreRequiredModules, overcattedEntities)
		).flatten
	) ++ previousYearsSCYDs.map(thisSCYD => {
		// We need to show the same row in previous years for each of the overcatted entities, but with the identifier of each overcatted entity
		val entityList = overcattedEntities.map(overcattedEntity => thisSCYD.toGenerateExamGridEntity(Some(overcattedEntity.id)))
		thisSCYD -> Seq(
			new CoreModulesColumnOption().getColumns(departmentCoreRequiredModules, entityList),
			new CoreRequiredModulesColumnOption().getColumns(departmentCoreRequiredModules, entityList),
			new CoreOptionalModulesColumnOption().getColumns(departmentCoreRequiredModules, entityList),
			new OptionalModulesColumnOption().getColumns(departmentCoreRequiredModules, entityList)
		).flatten
	}).toMap

	lazy val columnsBySCYDCategories: Map[StudentCourseYearDetails, Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]]] =
		columnsBySCYD.mapValues(columns => columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category))

	lazy val columnsBySCYDValues: Map[StudentCourseYearDetails, Seq[Map[String, String]]] =
		columnsBySCYD.mapValues(columns => columns.map(_.render))

}

trait GenerateExamGridOvercatCommandRequest {

	self: GenerateExamGridOvercatCommandState =>

	var overcatChoice: String = _

	def chosenModuleSubset: Option[(BigDecimal, Seq[ModuleRegistration])] =
		overcattedMarks.find{case(_, modules) => GenerateExamGridOvercatCommand.overcatIdentifier(modules) == Option(overcatChoice).getOrElse("")}

}

class ChooseOvercatColumnOption extends columns.ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "chooseovercat"

	override val sortOrder: Int = 0

	case class Column(entities: Seq[GenerateExamGridEntity], selectedEntityId: String) extends ExamGridColumn(entities) {

		override val title: String = ""

		override def render: Map[String, String] =
			entities.map(entity => entity.id -> "<input type=\"radio\" name=\"overcatChoice\" value=\"%s\" %s />".format(
				entity.id,
				if (selectedEntityId == entity.id) "checked" else ""
			)).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			row.createCell(index)
		}

	}

	override def getColumns(entities: Seq[GenerateExamGridEntity]): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumns(entities: Seq[GenerateExamGridEntity], selectedEntityId: String): Seq[ExamGridColumn] = Seq(Column(entities, selectedEntityId))

}
