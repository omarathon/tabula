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
import uk.ac.warwick.tabula.exams.grids.columns.marking.OvercattedYearMarkColumnOption
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreModulesColumnOption, CoreOptionalModulesColumnOption, CoreRequiredModulesColumnOption, OptionalModulesColumnOption}
import uk.ac.warwick.tabula.exams.grids.columns.{ExamGridColumn, ExamGridColumnOption, ExamGridColumnState, HasExamGridColumnCategory}
import uk.ac.warwick.tabula.helpers.StringUtils._
import uk.ac.warwick.tabula.services.{AutowiringUpstreamRouteRuleServiceComponent, AutowiringModuleRegistrationServiceComponent, ModuleRegistrationService, ModuleRegistrationServiceComponent}
import uk.ac.warwick.tabula.web.Mav
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.views.{JSONErrorView, JSONView}
import uk.ac.warwick.tabula.{AcademicYear, CurrentUser}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate/overcatting/{scyd}"))
class OvercattingOptionsController extends ExamsController
	with AutowiringModuleRegistrationServiceComponent with AutowiringUpstreamRouteRuleServiceComponent {

	validatesSelf[SelfValidating]

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	private def normalLoad(scyd: StudentCourseYearDetails, academicYear: AcademicYear) = {
		upstreamRouteRuleService.findNormalLoad(scyd.studentCourseDetails.route, academicYear, scyd.yearOfStudy).getOrElse(
			ModuleRegistrationService.DefaultNormalLoad
		)
	}

	@ModelAttribute("command")
	def command(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable scyd: StudentCourseYearDetails) =
		GenerateExamGridOvercatCommand(
			mandatory(department),
			mandatory(academicYear),
			mandatory(scyd),
			normalLoad(scyd, academicYear),
			user
		)

	@ModelAttribute("overcatView")
	def overcatView(@PathVariable department: Department, @PathVariable academicYear: AcademicYear, @PathVariable scyd: StudentCourseYearDetails) =
		OvercattingOptionsView(department, academicYear, scyd, normalLoad(scyd, academicYear))

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
	def apply(department: Department, academicYear: AcademicYear, scyd: StudentCourseYearDetails, normalLoad: BigDecimal) =
		new OvercattingOptionsView(department, academicYear, scyd, normalLoad)
		with AutowiringModuleRegistrationServiceComponent
		with GenerateExamGridOvercatCommandState
		with GenerateExamGridOvercatCommandRequest
}

class OvercattingOptionsView(val department: Department, val academicYear: AcademicYear, val scyd: StudentCourseYearDetails, val normalLoad: BigDecimal) {

	self: GenerateExamGridOvercatCommandState with GenerateExamGridOvercatCommandRequest with ModuleRegistrationServiceComponent =>

	override val user: CurrentUser = null // Never used

	private lazy val coreRequiredModules = moduleRegistrationService.findCoreRequiredModules(
		scyd.studentCourseDetails.currentRoute,
		academicYear,
		scyd.yearOfStudy
	).map(_.module)

	private lazy val overcattedMarks: Seq[(BigDecimal, Seq[ModuleRegistration])] =
		moduleRegistrationService.overcattedModuleSubsets(scyd.toGenerateExamGridEntity(), overwrittenMarks, normalLoad)

	lazy val overcattedEntities = overcattedMarks.map{case(mark, overcattedModules) => GenerateExamGridEntity(
		GenerateExamGridOvercatCommand.overcatIdentifier(overcattedModules),
		null,
		null,
		overcattedModules,
		overcattedModules.map(mr => BigDecimal(mr.cats)).sum,
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
		scyd.moduleRegistrations.map(mr => BigDecimal(mr.cats)).sum,
		Some(scyd.moduleRegistrations.map(_.module)),
		Some(overwrittenMarks),
		None
	)}

	private lazy val overcattedEntitiesState = ExamGridColumnState(
		entities = overcattedEntities,
		overcatSubsets = Map(), // Not used
		coreRequiredModules = coreRequiredModules,
		normalLoad = normalLoad,
		routeRules = Seq(), // Not used
		yearOfStudy = scyd.yearOfStudy
	)

	private lazy val allModulesEntitiesState = ExamGridColumnState(
		entities = allModulesEntities,
		overcatSubsets = Map(), // Not used
		coreRequiredModules = coreRequiredModules,
		normalLoad = normalLoad,
		routeRules = Seq(), // Not used
		yearOfStudy = scyd.yearOfStudy
	)

	private lazy val currentYearMark = moduleRegistrationService.weightedMeanYearMark(scyd.moduleRegistrations, overwrittenMarks)

	lazy val optionsColumns: Seq[ExamGridColumn] = Seq(
		new ChooseOvercatColumnOption().getColumns(overcattedEntitiesState, overcatChoice.maybeText.getOrElse("")),
		new OvercattedYearMarkColumnOption().getColumns(overcattedEntitiesState),
		new FixedValueCurrentYearMarkColumnOption().getColumns(allModulesEntitiesState, currentYearMark)
	).flatten

	lazy val optionsColumnsCategories: Map[String, Seq[ExamGridColumn with HasExamGridColumnCategory]] =
		optionsColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)

	lazy val optionsColumnValues: Seq[Map[String, String]] = optionsColumns.map(_.render)

	lazy val columnsBySCYD: Map[StudentCourseYearDetails, Seq[ExamGridColumn]] = Map(
		scyd -> Seq(
			new CoreModulesColumnOption().getColumns(overcattedEntitiesState),
			new CoreRequiredModulesColumnOption().getColumns(overcattedEntitiesState),
			new CoreOptionalModulesColumnOption().getColumns(overcattedEntitiesState),
			new OptionalModulesColumnOption().getColumns(overcattedEntitiesState)
		).flatten
	) ++ previousYearsSCYDs.map(thisSCYD => {
		// We need to show the same row in previous years for each of the overcatted entities, but with the identifier of each overcatted entity
		val entityList = overcattedEntities.map(overcattedEntity => thisSCYD.toGenerateExamGridEntity(Some(overcattedEntity.id)))
		val state = ExamGridColumnState(
			entities = entityList,
			overcatSubsets = Map(), // Not used
			coreRequiredModules = coreRequiredModules,
			normalLoad = normalLoad,
			routeRules = Seq(), // Not used
			yearOfStudy = thisSCYD.yearOfStudy
		)
		thisSCYD -> Seq(
			new CoreModulesColumnOption().getColumns(state),
			new CoreRequiredModulesColumnOption().getColumns(state),
			new CoreOptionalModulesColumnOption().getColumns(state),
			new OptionalModulesColumnOption().getColumns(state)
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

	case class Column(state: ExamGridColumnState, selectedEntityId: String) extends ExamGridColumn(state) {

		override val title: String = ""

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> "<input type=\"radio\" name=\"overcatChoice\" value=\"%s\" %s />".format(
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

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumns(state: ExamGridColumnState, selectedEntityId: String): Seq[ExamGridColumn] = Seq(Column(state, selectedEntityId))

}

class FixedValueCurrentYearMarkColumnOption extends ExamGridColumnOption with AutowiringModuleRegistrationServiceComponent {

	override val identifier: ExamGridColumnOption.Identifier = "currentyear"

	override val sortOrder: Int = ExamGridColumnOption.SortOrders.CurrentYear

	override val mandatory = true

	case class Column(state: ExamGridColumnState, value: Option[BigDecimal]) extends ExamGridColumn(state) with HasExamGridColumnCategory {

		override val title: String = "Weighted Mean Module Mark"

		override val category: String = s"Year ${state.yearOfStudy} Marks"

		override def render: Map[String, String] =
			state.entities.map(entity => entity.id -> value.map(_.toString).getOrElse("")).toMap

		override def renderExcelCell(
			row: XSSFRow,
			index: Int,
			entity: GenerateExamGridEntity,
			cellStyleMap: Map[GenerateExamGridExporter.Style, XSSFCellStyle]
		): Unit = {
			val cell = row.createCell(index)
			value.foreach(mark =>
				cell.setCellValue(mark.doubleValue())
			)
		}

	}

	override def getColumns(state: ExamGridColumnState): Seq[ExamGridColumn] = throw new UnsupportedOperationException

	def getColumns(state: ExamGridColumnState, value: Option[BigDecimal]): Seq[ExamGridColumn] = Seq(Column(state, value))

}
