package uk.ac.warwick.tabula.web.controllers.exams.grids.generate

import javax.validation.Valid

import org.joda.time.DateTime
import org.springframework.stereotype.Controller
import org.springframework.validation.{BindException, Errors}
import org.springframework.web.bind.annotation.{ModelAttribute, PathVariable, RequestMapping, RequestParam}
import uk.ac.warwick.spring.Wire
import uk.ac.warwick.tabula.JavaImports._
import uk.ac.warwick.tabula.commands.exams.grids._
import uk.ac.warwick.tabula.commands.{Appliable, FilterStudentsOrRelationships, SelfValidating, TaskBenchmarking}
import uk.ac.warwick.tabula.data.AutowiringCourseDaoComponent
import uk.ac.warwick.tabula.data.model.StudentCourseYearDetails.YearOfStudy
import uk.ac.warwick.tabula.data.model._
import uk.ac.warwick.tabula.exams.grids.columns._
import uk.ac.warwick.tabula.exams.grids.columns.modules.{CoreRequiredModulesColumnOption, ModuleReportsColumnOption}
import uk.ac.warwick.tabula.helpers.DateTimeOrdering._
import uk.ac.warwick.tabula.jobs.scheduling.ImportMembersJob
import uk.ac.warwick.tabula.permissions.{Permission, Permissions}
import uk.ac.warwick.tabula.services._
import uk.ac.warwick.tabula.services.jobs.AutowiringJobServiceComponent
import uk.ac.warwick.tabula.web.controllers.exams.ExamsController
import uk.ac.warwick.tabula.web.controllers.{AcademicYearScopedController, DepartmentScopedController}
import uk.ac.warwick.tabula.web.views.JSONView
import uk.ac.warwick.tabula.web.{Mav, Routes}
import uk.ac.warwick.tabula.{AcademicYear, ItemNotFoundException}

import scala.collection.JavaConverters._

object GenerateExamGridMappingParameters {
	final val selectCourse = "selectCourse"
	final val gridOptions = "gridOptions"
	final val coreRequiredModules = "coreRequiredModules"
	final val previewAndDownload = "previewAndDownload"
	final val excel = "excel"
	final val marksRecord = "marksRecord"
	final val marksRecordConfidential = "marksRecordConfidential"
	final val transcript = "transcript"
	final val transcriptConfidential = "transcriptConfidential"
}

@Controller
@RequestMapping(Array("/exams/grids/{department}/{academicYear}/generate"))
class GenerateExamGridController extends ExamsController
	with DepartmentScopedController with AcademicYearScopedController
	with AutowiringUserSettingsServiceComponent with AutowiringModuleAndDepartmentServiceComponent
	with AutowiringMaintenanceModeServiceComponent with AutowiringJobServiceComponent
	with AutowiringCourseDaoComponent with AutowiringModuleRegistrationServiceComponent
	with ExamGridDocumentsController
	with TaskBenchmarking {

	type SelectCourseCommand = Appliable[Seq[ExamGridEntity]] with GenerateExamGridSelectCourseCommandRequest with GenerateExamGridSelectCourseCommandState
	type GridOptionsCommand = Appliable[(Set[ExamGridColumnOption.Identifier], Seq[String])] with GenerateExamGridGridOptionsCommandRequest
	type CoreRequiredModulesCommand = Appliable[Seq[CoreRequiredModule]] with PopulateGenerateExamGridSetCoreRequiredModulesCommand
	type CheckOvercatCommand = Appliable[GenerateExamGridCheckAndApplyOvercatCommand.Result] with GenerateExamGridCheckAndApplyOvercatCommandState with SelfValidating

	override val departmentPermission: Permission = Permissions.Department.ExamGrids

	@ModelAttribute("activeDepartment")
	override def activeDepartment(@PathVariable department: Department): Option[Department] = retrieveActiveDepartment(Option(department))

	@ModelAttribute("activeAcademicYear")
	override def activeAcademicYear(@PathVariable academicYear: AcademicYear): Option[AcademicYear] = retrieveActiveAcademicYear(Option(academicYear))

	private lazy val allExamGridsColumns: Seq[ExamGridColumnOption] = Wire.all[ExamGridColumnOption].sorted

	validatesSelf[SelfValidating]

	private def commonCrumbs(view: Mav, department: Department, academicYear: AcademicYear): Mav =
		view.crumbs(Breadcrumbs.Grids.Home, Breadcrumbs.Grids.Department(department, academicYear))
			.secondCrumbs(academicYearBreadcrumbs(academicYear)(year => Routes.exams.Grids.generate(department, year)): _*)

	@ModelAttribute("selectCourseCommand")
	def selectCourseCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSelectCourseCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("gridOptionsCommand")
	def gridOptionsCommand = GenerateExamGridGridOptionsCommand()

	@ModelAttribute("coreRequiredModulesCommand")
	def coreRequiredModulesCommand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridSetCoreRequiredModulesCommand(mandatory(department), mandatory(academicYear))

	@ModelAttribute("checkOvercatCommmand")
	def checkOvercatCommmand(@PathVariable department: Department, @PathVariable academicYear: AcademicYear) =
		GenerateExamGridCheckAndApplyOvercatCommand(department, academicYear, user)

	@ModelAttribute("GenerateExamGridMappingParameters")
	def params = GenerateExamGridMappingParameters

	@ModelAttribute("coreRequiredModules")
	def coreRequiredModules(
		@PathVariable academicYear: AcademicYear,
		@RequestParam(value = "route", required = false) route: Route,
		@RequestParam(value = "yearOfStudy", required = false) yearOfStudy: JInteger
	) = {
		if (Option(route).nonEmpty && Option(yearOfStudy).nonEmpty) {
			moduleRegistrationService.findCoreRequiredModules(route, academicYear, yearOfStudy)
		} else {
			Seq()
		}
	}

	@ModelAttribute("ExamGridColumnValueType")
	def examGridColumnValueType = ExamGridColumnValueType

	@RequestMapping(method = Array(GET, POST))
	def selectCourseRender(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/selectCourse"),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.selectCourse))
	def selectCourseSubmit(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		errors: Errors,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			selectCourseRender(selectCourseCommand, department, academicYear)
		} else {
			val students = selectCourseCommand.apply()
			if (students.isEmpty) {
				errors.reject("examGrid.noStudents")
				selectCourseRender(selectCourseCommand, department, academicYear)
			} else {
				val jobInstance = jobService.add(Some(user), ImportMembersJob(students.map(_.universityId)))
				if (gridOptionsCommand.predefinedColumnIdentifiers.isEmpty) {
					gridOptionsCommand.predefinedColumnIdentifiers.addAll(allExamGridsColumns.map(_.identifier).asJava)
				}
				gridOptionsRender(jobInstance.id, selectCourseCommand, department, academicYear)
			}
		}
	}

	private def gridOptionsRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(Mav("exams/grids/generate/gridOption", "jobId" -> jobId), department, academicYear)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.gridOptions))
	def gridOptions(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		errors: Errors,
		@ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModules") coreRequiredModules: Seq[CoreRequiredModule],
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			gridOptionsRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val columnIDs = gridOptionsCommand.apply()
			if (columnIDs._1.contains(new CoreRequiredModulesColumnOption().identifier) || columnIDs._1.contains(new ModuleReportsColumnOption().identifier)) {
				coreRequiredModulesCommand.populate()
				coreRequiredModulesRender(jobId, selectCourseCommand, department, academicYear)
			} else {
				checkJobProgress(
					jobId,
					selectCourseCommand,
					gridOptionsCommand,
					checkOvercatCommmand,
					coreRequiredModules,
					department,
					academicYear
				)
			}
		}
	}

	private def coreRequiredModulesRender(jobId: String, selectCourseCommand: SelectCourseCommand, department: Department, academicYear: AcademicYear): Mav = {
		commonCrumbs(
			Mav("exams/grids/generate/coreRequiredModules", "jobId" -> jobId),
			department,
			academicYear
		)
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.coreRequiredModules))
	def coreRequiredModules(
		@ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		@ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		@Valid @ModelAttribute("coreRequiredModulesCommand") coreRequiredModulesCommand: CoreRequiredModulesCommand,
		errors: Errors,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@RequestParam jobId: String,
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (errors.hasErrors) {
			coreRequiredModulesRender(jobId, selectCourseCommand, department, academicYear)
		} else {
			val coreRequiredModules = coreRequiredModulesCommand.apply()
			checkJobProgress(
				jobId,
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommmand,
				coreRequiredModules,
				department,
				academicYear
			)
		}
	}

	private def checkJobProgress(
		jobId: String,
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommmand: CheckOvercatCommand,
		coreRequiredModules: Seq[CoreRequiredModule],
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val jobInstance = jobService.getInstance(jobId)
		if (jobInstance.isDefined && !jobInstance.get.finished) {
			val studentLastImportDates = selectCourseCommand.apply().map(e =>
				(Seq(e.firstName, e.lastName).mkString(" "), e.lastImportDate.getOrElse(new DateTime(0)))
			).sortBy(_._2)
			commonCrumbs(
				Mav("exams/grids/generate/jobProgress",
					"jobId" -> jobId,
					"jobProgress" -> jobInstance.get.progress,
					"jobStatus" -> jobInstance.get.status,
					"studentLastImportDates" -> studentLastImportDates
				),
				department,
				academicYear
			)
		} else {
			previewAndDownloadRender(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommmand,
				coreRequiredModules,
				department,
				academicYear
			)
		}
	}

	@RequestMapping(method = Array(POST), value = Array("/progress"))
	def jobProgress(@RequestParam jobId: String): Mav = {
		jobService.getInstance(jobId).map(jobInstance =>
			Mav(new JSONView(Map(
				"id" -> jobInstance.id,
				"status" -> jobInstance.status,
				"progress" -> jobInstance.progress,
				"finished" -> jobInstance.finished
			))).noLayout()
		).getOrElse(throw new ItemNotFoundException())
	}

	@RequestMapping(method = Array(POST), params = Array(GenerateExamGridMappingParameters.previewAndDownload))
	def previewAndDownload(
		@Valid @ModelAttribute("selectCourseCommand") selectCourseCommand: SelectCourseCommand,
		selectCourseCommandErrors: Errors,
		@Valid @ModelAttribute("gridOptionsCommand") gridOptionsCommand: GridOptionsCommand,
		gridOptionsCommandErrors: Errors,
		@ModelAttribute("checkOvercatCommmand") checkOvercatCommmand: CheckOvercatCommand,
		@ModelAttribute("coreRequiredModules") coreRequiredModules: Seq[CoreRequiredModule],
		@PathVariable department: Department,
		@PathVariable academicYear: AcademicYear
	): Mav = {
		if (selectCourseCommandErrors.hasErrors || gridOptionsCommandErrors.hasErrors) {
			throw new IllegalArgumentException
		}
		previewAndDownloadRender(
			selectCourseCommand,
			gridOptionsCommand,
			checkOvercatCommmand,
			coreRequiredModules,
			department,
			academicYear
		)
	}

	private def previewAndDownloadRender(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCommmand: CheckOvercatCommand,
		coreRequiredModules: Seq[CoreRequiredModule],
		department: Department,
		academicYear: AcademicYear
	): Mav = {
		val GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadOption, routeRules) = benchmarkTask("GridData") {
			checkAndApplyOvercatAndGetGridData(
				selectCourseCommand,
				gridOptionsCommand,
				checkOvercatCommmand,
				coreRequiredModules
			)
		}

		val oldestImport = benchmarkTask("oldestImport") { entities.flatMap(_.lastImportDate).reduceOption((a, b) => if(a.isBefore(b)) a else b) }
		val chosenYearColumnValues = benchmarkTask("chosenYearColumnValues") { Seq(studentInformationColumns, summaryColumns).flatten.map(c => c -> c.values).toMap }
		val chosenYearColumnCategories = benchmarkTask("chosenYearColumnCategories") { summaryColumns.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category) }
		val perYearColumnValues = benchmarkTask("perYearColumnValues") { perYearColumns.values.flatten.toSeq.map(c => c -> c.values).toMap }
		val perYearColumnCategories = benchmarkTask("perYearColumnCategories") { perYearColumns.mapValues(_.collect{case c: HasExamGridColumnCategory => c}.groupBy(_.category)) }

		commonCrumbs(
			Mav("exams/grids/generate/preview",
				"oldestImport" -> oldestImport,
				"studentInformationColumns" -> studentInformationColumns,
				"perYearColumns" -> perYearColumns,
				"summaryColumns" -> summaryColumns,
				"chosenYearColumnValues" -> chosenYearColumnValues,
				"perYearColumnValues" -> perYearColumnValues,
				"chosenYearColumnCategories" -> chosenYearColumnCategories,
				"perYearColumnCategories" -> perYearColumnCategories,
				"entities" -> entities,
				"generatedDate" -> DateTime.now,
				"weightings" -> weightings,
				"normalLoadOption" -> normalLoadOption,
				"defaultNormalLoad" -> selectCourseCommand.route.degreeType.normalCATSLoad,
				"routeRules" -> routeRules
			),
			department,
			academicYear
		)
	}

	protected case class GridData(
		entities: Seq[ExamGridEntity],
		studentInformationColumns: Seq[ChosenYearExamGridColumn],
		perYearColumns: Map[YearOfStudy, Seq[PerYearExamGridColumn]],
		summaryColumns: Seq[ChosenYearExamGridColumn],
		weightings: Seq[CourseYearWeighting],
		normalLoadOption: Option[BigDecimal],
		routeRules: Seq[UpstreamRouteRule]
	)

	protected def checkAndApplyOvercatAndGetGridData(
		selectCourseCommand: SelectCourseCommand,
		gridOptionsCommand: GridOptionsCommand,
		checkOvercatCmd: CheckOvercatCommand,
		coreRequiredModules: Seq[CoreRequiredModule]
	): GridData = {

		checkOvercatCmd.selectCourseCommand = selectCourseCommand
		checkOvercatCmd.yearsToShow = gridOptionsCommand.yearsToShow
		val checkOvercatCommmandErrors = new BindException(selectCourseCommand, "checkOvercatCommand")
		checkOvercatCmd.validate(checkOvercatCommmandErrors)

		val entities = {
			if (checkOvercatCommmandErrors.hasErrors) {
				checkOvercatCmd.entities
			} else {
				checkOvercatCmd.apply().entities
			}
		}

		val normalLoadOption = checkOvercatCmd.normalLoadOption
		val normalLoad = checkOvercatCmd.normalLoad
		val routeRules = checkOvercatCmd.routeRules

		val gridOptions = gridOptionsCommand.apply()
		val predefinedColumnIDs = gridOptions._1
		val customColumnTitles = gridOptions._2

		val state = ExamGridColumnState(
			entities = entities,
			overcatSubsets = entities.flatMap(_.years.get(selectCourseCommand.yearOfStudy)).map(entityYear => entityYear ->
				moduleRegistrationService.overcattedModuleSubsets(entityYear, entityYear.markOverrides.getOrElse(Map()), normalLoad, routeRules)
			).toMap,
			coreRequiredModules = coreRequiredModules.map(_.module),
			normalLoad = normalLoad,
			routeRules = routeRules,
			academicYear = selectCourseCommand.academicYear,
			yearOfStudy = selectCourseCommand.yearOfStudy,
			showFullName = gridOptionsCommand.showFullName,
			showComponentMarks = gridOptionsCommand.showComponentMarks,
			showModuleNames = gridOptionsCommand.showModuleNames
		)

		val predefinedColumnOptions = allExamGridsColumns.filter(c => c.mandatory || predefinedColumnIDs.contains(c.identifier))
		val studentInformationColumns = predefinedColumnOptions.collect { case c: StudentExamGridColumnOption => c }.flatMap(_.getColumns(state))
		val summaryColumns = predefinedColumnOptions.collect { case c: ChosenYearExamGridColumnOption => c }.flatMap(_.getColumns(state)) ++
			customColumnTitles.flatMap(BlankColumnOption.getColumn)
		val perYearColumns = predefinedColumnOptions.collect { case c: PerYearExamGridColumnOption => c }
			.flatMap(_.getColumns(state).toSeq)
			.groupBy { case (year, _) => year}
			.mapValues(_.flatMap { case (_, columns) => columns })

		val weightings = (1 to FilterStudentsOrRelationships.MaxYearsOfStudy).flatMap(year =>
			courseDao.getCourseYearWeighting(selectCourseCommand.course.code, selectCourseCommand.academicYear, year)
		).sorted

		GridData(entities, studentInformationColumns, perYearColumns, summaryColumns, weightings, normalLoadOption, routeRules)
	}

}
