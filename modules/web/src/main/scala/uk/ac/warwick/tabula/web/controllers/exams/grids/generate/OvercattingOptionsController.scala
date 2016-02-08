package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.apache.poi.xssf.usermodel.{XSSFCellStyle, XSSFRow}
import org.springframework.stereotype.Controller
import org.springframework.validation.Errors
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping}
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, PopulateOnForm, SelfValidating}
import uk.ac.warwick.tabula.data.model.{Department, Module, ModuleRegistration, StudentCourseYearDetails}
import uk.ac.warwick.tabula.exams.grids.columns
import uk.ac.warwick.tabula.exams.grids.columns.marking.{CurrentYearMarkColumnOption, OvercattedYearMarkColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreModulesColumnOption, CoreOptionalModulesColumnOption, CoreRequiredModulesColumnOption, OptionalModulesColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringModuleRegistrationServiceComponent, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/overcatting/{scyd}"))
class OvercattingOptionsController extends ExamsController with AutowiringModuleRegistrationServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable scyd: StudentCourseYearDetails) =
		GenerateExamGridOvercatCommand(mandatory(department), mandatory(academicYear), mandatory(scyd), user)

	@ModelAttribute("overcatView")
	def overcatView(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable scyd: StudentCourseYearDetails) =
		OvercattingOptionsView(department, academicYear, scyd)

	@RequestMapping(method = Array(GET))
	def form(
		@ModelAttribute("command") cmd: Appliable[Seq[Module]] with PopulateOnForm,
		@ModelAttribute("overcatView") overcatView: OvercattingOptionsView with GenerateExamGridOvercatCommandRequest,
		@PathVariable scyd: StudentCourseYearDetails
	): Mav = {
		cmd.populate()
		overcatView.overcatChoice = scyd.overcattingModules.map(overcattingModules =>
			GenerateExamGridOvercatCommand.overcatIdentifier(
				scyd.moduleRegistrations.filter(mr => overcattingModules.contains(mr.module))
			)
		).getOrElse("")
		scyd.overcattingMarkOverrides.foreach(overcattingMarkOverrides =>
			overcattingMarkOverrides.foreach{ case(module, mark) => overcatView.newModuleMarks.put(module, mark.toString) }
		)
		Mav("exams/grids/generate/overcat").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), params = Array("recalculate"))
	def recalculate(
		@ModelAttribute("command") cmd: Appliable[Seq[Module]],
		@ModelAttribute("overcatView") overcatView: OvercattingOptionsView
	) = {
		Mav("exams/grids/generate/overcat").noLayoutIf(ajax)
	}

	@RequestMapping(method = Array(POST), params = Array("continue"))
	def submit(@Valid @ModelAttribute("command") cmd: Appliable[Seq[Module]], errors: Errors) = {
		if (errors.hasErrors) {
			new JSONErrorView(errors)
		} else {
			new JSONView(Map(
				"modules" -> cmd.apply().map(_.code)
			))
		}
	}

}

object OvercattingOptionsView {
	def apply(department: Department, academicYear: AcademicYear, scyd: StudentCourseYearDetails) =
		new OvercattingOptionsView(department, academicYear, scyd)
		with AutowiringModuleRegistrationServiceComponent
		with GenerateExamGridOvercatCommandState
		with GenerateExamGridOvercatCommandRequest
}

class OvercattingOptionsView(val department: Department, val academicYear: AcademicYear, val scyd: StudentCourseYearDetails) {

	self: GenerateExamGridOvercatCommandState with GenerateExamGridOvercatCommandRequest with ModuleRegistrationServiceComponent =>

	override val user: CurrentUser = null // Never used

	private lazy val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(
		scyd.studentCourseDetails.route,
		academicYear,
		scyd.yearOfStudy
	).map(_.module)

	private lazy val overcattedMarks: Seq[(BigDecimal, Seq[ModuleRegistration])] =
		moduleRegistrationService.overcattedModuleSubsets(scyd.toGenerateExamGridEntity(), overwrittenMarks)

	lazy val overcattedEntities = overcattedMarks.map{case(mark, overcattedModules) => GenerateExamGridEntity(
		GenerateExamGridOvercatCommand.overcatIdentifier(overcattedModules),
		null,
		null,
		overcattedModules,
		scyd.normalCATLoad,
		Some(overcattedModules.map(_.module)),
		Some(overwrittenMarks),
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
		Some(overwrittenMarks),
		None
	)}

	lazy val optionsColumns: Seq[ExamGridColumn] = Seq(
		new ChooseOvercatColumnOption().getColumns(overcattedEntities, overcatChoice.maybeText.getOrElse("")),
		new OvercattedYearMarkColumnOption().getColumns(scyd.yearOfStudy, overcattedEntities),
		new CurrentYearMarkColumnOption().getColumns(scyd.yearOfStudy, allModulesEntities)
	).flatten

	lazy val optionsColumnsCategories: Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]] =
		optionsColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

	lazy val optionsColumnValues: Seq[Map[String, String]] = optionsColumns.map(_.render)

	lazy val columnsBySCYD: Map[StudentCourseYearDetails, Seq[ExamGridColumn]] = Map(
		scyd -> Seq(
			new CoreModulesColumnOption().getColumns(coreRequiredModules, overcattedEntities),
			new CoreRequiredModulesColumnOption().getColumns(coreRequiredModules, overcattedEntities),
			new CoreOptionalModulesColumnOption().getColumns(coreRequiredModules, overcattedEntities),
			new OptionalModulesColumnOption().getColumns(coreRequiredModules, overcattedEntities)
		).flatten
	) ++ previousYearsSCYDs.map(thisSCYD => {
		// We need to show the same row in previous years for each of the overcatted entities, but with the identifier of each overcatted entity
		val entityList = overcattedEntities.map(overcattedEntity => thisSCYD.toGenerateExamGridEntity(Some(overcattedEntity.id)))
		thisSCYD -> Seq(
			new CoreModulesColumnOption().getColumns(coreRequiredModules, entityList),
			new CoreRequiredModulesColumnOption().getColumns(coreRequiredModules, entityList),
			new CoreOptionalModulesColumnOption().getColumns(coreRequiredModules, entityList),
			new OptionalModulesColumnOption().getColumns(coreRequiredModules, entityList)
		).flatten
	}).toMap

	lazy val columnsBySCYDCategories: Map[StudentCourseYearDetails, Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]]] =
		columnsBySCYD.mapValues(columns => columns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category))

	lazy val columnsBySCYDValues: Map[StudentCourseYearDetails, Seq[Map[String, String]]] =
		columnsBySCYD.mapValues(columns => columns.map(_.render))

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
